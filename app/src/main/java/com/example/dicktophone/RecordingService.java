package com.example.dicktophone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {

    public static final String ACTION_START = "com.example.dicktophone.START";
    public static final String ACTION_STOP  = "com.example.dicktophone.STOP";
    private static final String CHANNEL_ID  = "recorder_channel";

    private MediaRecorder recorder;
    private ParcelFileDescriptor currentPfd;
    private boolean recording = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                if (!checkFreeSpace()) {
                    Toast.makeText(this, R.string.error_no_space, Toast.LENGTH_LONG).show();
                    stopSelf();
                    return START_NOT_STICKY;
                }
                startRecordingInternal();
            } else if (ACTION_STOP.equals(action)) {
                stopRecordingInternal();
            }
        }
        return START_STICKY;
    }

    private boolean checkFreeSpace() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long bytesAvailable = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        return bytesAvailable > 5 * 1024 * 1024; // Минимум 5 МБ
    }

    private void startRecordingInternal() {
        if (recording) return;
        createChannel();

        PendingIntent stopPi = PendingIntent.getService(
                this, 1,
                new Intent(this, RecordingService.class).setAction(ACTION_STOP),
                Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .addAction(new NotificationCompat.Action(0, getString(R.string.notif_stop), stopPi))
                .build();

        startForeground(1, notif);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String dirUriStr = sp.getString("pref_output_dir", null);
        String durationStr = sp.getString("pref_max_duration_min", "60");
        String quality = sp.getString("pref_quality", "high");

        int minutes = 60;
        try { minutes = Math.max(1, Integer.parseInt(durationStr)); } catch (Exception ignored) {}

        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "REC_" + time + ".m4a";

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        // Настройка качества
        if ("low".equals(quality)) {
            recorder.setAudioEncodingBitRate(64000);
            recorder.setAudioSamplingRate(22050);
        } else {
            recorder.setAudioEncodingBitRate(128000);
            recorder.setAudioSamplingRate(44100);
        }

        recorder.setOnInfoListener((mr, what, extra) -> {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                boolean loop = sp.getBoolean("pref_loop", false);
                stopRecordingInternal();
                if (loop) {
                    // Перезапуск для циклической записи
                    Intent i = new Intent(this, RecordingService.class);
                    i.setAction(ACTION_START);
                    startService(i);
                }
            }
        });

        long maxMs = minutes * 60L * 1000L;
        if (maxMs > Integer.MAX_VALUE) maxMs = Integer.MAX_VALUE;
        recorder.setMaxDuration((int) maxMs);

        try {
            if (dirUriStr != null) {
                Uri tree = Uri.parse(dirUriStr);
                DocumentFile dir = DocumentFile.fromTreeUri(this, tree);
                if (dir == null || !dir.canWrite()) throw new IOException("No write access");
                DocumentFile out = dir.createFile("audio/mp4", fileName);
                if (out == null) throw new IOException("Create file failed");
                currentPfd = getContentResolver().openFileDescriptor(out.getUri(), "w");
                if (currentPfd == null) throw new IOException("PFD null");
                recorder.setOutputFile(currentPfd.getFileDescriptor());
            } else {
                File folder = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                if (folder != null && !folder.exists()) folder.mkdirs();
                File out = new File(folder, fileName);
                recorder.setOutputFile(out.getAbsolutePath());
            }

            recorder.prepare();
            recorder.start();
            recording = true;
        } catch (Exception e) {
            stopRecordingInternal();
        }
    }

    private void stopRecordingInternal() {
        if (!recording) {
            stopForeground(true);
            stopSelf();
            return;
        }
        try { recorder.stop(); } catch (Exception e) { e.printStackTrace(); }
        try { recorder.reset(); recorder.release(); } catch (Exception ignored) {}

        if (currentPfd != null) {
            try { currentPfd.close(); } catch (IOException e) { e.printStackTrace(); }
            currentPfd = null;
        }

        recording = false;
        stopForeground(true);
        stopSelf();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Voice Recorder", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }
}