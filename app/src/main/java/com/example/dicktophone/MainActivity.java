package com.example.dicktophone;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_AUDIO = 100;
    private static final int REQ_POST_NOTIF = 101;

    private TextView statusText;
    private TextView timerText;
    private TextView folderText;
    private Handler handler;
    private long startMs = 0L;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusText = findViewById(R.id.statusText);
        timerText = findViewById(R.id.timerText);
        folderText = findViewById(R.id.folderText);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);
        Button btnSettings = findViewById(R.id.btnSettings);
        Button btnList = findViewById(R.id.btnList);

        statusText.setText(R.string.status_ready);
        timerText.setText(R.string.timer_zero);
        btnStart.setText(R.string.btn_start);
        btnStop.setText(R.string.btn_stop);
        btnSettings.setText(R.string.btn_settings);

        handler = new Handler(Looper.getMainLooper());

        btnStart.setOnClickListener(v -> tryStartRecording());
        btnStop.setOnClickListener(v -> stopRecording());
        btnSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
        btnList.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RecordingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        showFolderSummary();
    }

    private void showFolderSummary() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String uri = sp.getString("pref_output_dir", null);
        if (uri == null) {
            folderText.setText(R.string.folder_internal);
        } else {
            folderText.setText(getString(R.string.folder_saf, Uri.parse(uri).getLastPathSegment()));
        }
    }

    private void tryStartRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIF);
            return;
        }
        startRecording();
    }

    private void startRecording() {
        Intent start = new Intent(this, RecordingService.class);
        start.setAction(RecordingService.ACTION_START);
        ContextCompat.startForegroundService(this, start);
        isRecording = true;
        statusText.setText(R.string.status_recording);
        startMs = System.currentTimeMillis();
        handler.post(tick);
    }

    private void stopRecording() {
        Intent stop = new Intent(this, RecordingService.class);
        stop.setAction(RecordingService.ACTION_STOP);
        ContextCompat.startForegroundService(this, stop);
        isRecording = false;
        statusText.setText(R.string.status_stopped);
        handler.removeCallbacks(tick);
        timerText.setText(R.string.timer_zero);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!isRecording) return;
            long sec = (System.currentTimeMillis() - startMs) / 1000L;
            long mm = sec / 60;
            long ss = sec % 60;
            timerText.setText(String.format("%02d:%02d", mm, ss));
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO || requestCode == REQ_POST_NOTIF) {
            boolean allGranted = true;
            for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) allGranted = false;
            if (allGranted) startRecording();
        }
    }
}