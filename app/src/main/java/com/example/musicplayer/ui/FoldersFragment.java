package com.example.musicplayer.ui;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.example.musicplayer.MediaDataManager;
import com.example.musicplayer.R;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.musicplayer.Track;
import com.example.musicplayer.QueueManager;
import com.example.musicplayer.service.PlaybackService;
import android.widget.Toast;
import com.example.musicplayer.LogUtil;

public class FoldersFragment extends Fragment {
    private ListView listView;
    private FolderAdapter adapter;
    private List<File> items = new ArrayList<>();
    private ExecutorService executor = Executors.newFixedThreadPool(3);
    private Handler handler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_folders, container, false);
        listView = root.findViewById(R.id.folder_list);
        adapter = new FolderAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    File selected = items.get(position);
                    if (selected.isDirectory()) {
                        loadFolder(selected);
                    } else if (selected.getName().toLowerCase().endsWith(".pdf")) {
                        openPdf(selected);
                    } else {
                        try{
                        // Add to queue and play
                        Track track = MediaDataManager.extractMetadata(selected.getAbsolutePath());
                        List<Track> list = new ArrayList<>();
                        list.add(track);
                        QueueManager.getInstance().setQueue(list, 0,getActivity());
                        //PlaybackService svc = ((MainActivity) getActivity()).getService();
                        //if (svc != null) svc.initTrack(track.getPath());
                        ((MainActivity) getActivity()).startService(new Intent(getActivity(),PlaybackService.class).setAction(PlaybackService. ACTION_INIT_TRACK).putExtra("path",track.getPath()));
                        }catch(Throwable e){LogUtil.logToFile(e);}
                    }
                }
            });

        loadFolder(Environment.getExternalStorageDirectory());
        
        return root;
    }

    private void loadFolder(final File folder) {
        executor.execute(new Runnable() {
                @Override
                public void run() {
                    File[] files = folder.listFiles();
                    final List<File> list = new ArrayList<>();
                    if (files != null) {
                        for (File f : files) {
                            if (f.isDirectory() || isAudioOrPdf(f)) list.add(f);
                        }
                    }
                    handler.post(new Runnable() {
                            @Override
                            public void run() {
                                items.clear();
                                items.addAll(list);
                                adapter.notifyDataSetChanged();
                            }
                        });
                }
            });
    }

    private boolean isAudioOrPdf(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".flac") ||
            name.endsWith(".ogg") || name.endsWith(".wav") ||
            name.endsWith(".m4a") || name.endsWith(".pdf");
    }
/*
    private void openPdf(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }*/
    
    private void openPdf(File file) {
        Uri uri = Uri.parse("content://" + getActivity().getPackageName() + ".fileprovider/" + file.getAbsolutePath());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
    class FolderAdapter extends BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int pos) { return items.get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.item_folder, parent, false);
            }
            File file = items.get(position);
            TextView name = convertView.findViewById(R.id.file_name);
            name.setText(file.getName());
            return convertView;
        }
    }
/*
    public void scanToDatabase() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                MediaDataManager mgr = new MediaDataManager(getActivity());
                scanDir(Environment.getExternalStorageDirectory(), mgr);
                return null;
            }
            void scanDir(File dir, MediaDataManager mgr) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isDirectory()) scanDir(f, mgr);
                        else if (isAudioOrPdf(f) && !f.getName().endsWith(".pdf")) {
                            // extract metadata and insert
                            // Use MediaMetadataRetriever
                            mgr.insertTrack(mgr.extractMetadata(f.getAbsolutePath()));
                        }
                    }
                }
            }
        }.execute();
    }*/
}

