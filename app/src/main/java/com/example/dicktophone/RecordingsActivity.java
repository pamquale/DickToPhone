package com.example.dicktophone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordingsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recordings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadFiles();
    }

    private void loadFiles() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String dirUriStr = sp.getString("pref_output_dir", null);
        List<RecordingItem> items = new ArrayList<>();

        if (dirUriStr != null) {
            // SAF
            Uri tree = Uri.parse(dirUriStr);
            DocumentFile dir = DocumentFile.fromTreeUri(this, tree);
            if (dir != null) {
                for (DocumentFile file : dir.listFiles()) {
                    if (file.getName() != null && file.getName().endsWith(".m4a")) {
                        items.add(new RecordingItem(file.getName(), file.getUri(), file.length()));
                    }
                }
            }
        } else {
            // Internal
            File folder = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (folder != null && folder.exists()) {
                for (File f : folder.listFiles()) {
                    if (f.getName().endsWith(".m4a")) {
                        items.add(new RecordingItem(f.getName(), Uri.fromFile(f), f.length()));
                    }
                }
            }
        }
        recyclerView.setAdapter(new Adapter(items));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) mediaPlayer.release();
    }

    private void playFile(Uri uri) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, uri);
        if (mediaPlayer != null) mediaPlayer.start();
        else Toast.makeText(this, "Помилка відтворення", Toast.LENGTH_SHORT).show();
    }

    private void shareFile(Uri uri) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("audio/mp4");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Поділитися"));
    }

    private static class RecordingItem {
        String name;
        Uri uri;
        long size;
        RecordingItem(String n, Uri u, long s) { name = n; uri = u; size = s; }
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        List<RecordingItem> list;
        Adapter(List<RecordingItem> list) { this.list = list; }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recording, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            RecordingItem item = list.get(position);
            holder.name.setText(item.name);
            holder.size.setText(item.size / 1024 + " KB");
            holder.itemView.setOnClickListener(v -> playFile(item.uri));
            holder.itemView.setOnLongClickListener(v -> {
                shareFile(item.uri);
                return true;
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView name, size;
            Holder(View v) {
                super(v);
                name = v.findViewById(R.id.fileName);
                size = v.findViewById(R.id.fileSize);
            }
        }
    }
}