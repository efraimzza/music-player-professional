package com.example.musicplayer.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.musicplayer.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsFragment extends Fragment {
    private ListView listView;
    private PlaylistAdapter adapter;
    private List<String> playlistNames = new ArrayList<>();
    private File playlistDir;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_playlists, container, false);
        listView = root.findViewById(R.id.list);
        playlistDir = new File(getActivity().getFilesDir(), "playlists");
        if (!playlistDir.exists()) playlistDir.mkdirs();
        adapter = new PlaylistAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // Load playlist tracks (stored as serialized list of paths)
                }
            });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    showDeleteDialog(position);
                    return true;
                }
            });

        root.findViewById(R.id.btn_new_playlist).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { showCreateDialog(); }
            });
        loadPlaylistNames();
        return root;
    }

    private void loadPlaylistNames() {
        playlistNames.clear();
        File[] files = playlistDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".mpl")) {
                    playlistNames.add(f.getName().replace(".mpl", ""));
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showCreateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("New Playlist");
        final EditText input = new EditText(getActivity());
        builder.setView(input);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        try {
                            new File(playlistDir, name + ".mpl").createNewFile();
                            loadPlaylistNames();
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteDialog(final int pos) {
        new AlertDialog.Builder(getActivity())
            .setMessage("Delete playlist?")
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new File(playlistDir, playlistNames.get(pos) + ".mpl").delete();
                    loadPlaylistNames();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    class PlaylistAdapter extends BaseAdapter {
        @Override public int getCount() { return playlistNames.size(); }
        @Override public Object getItem(int pos) { return playlistNames.get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            TextView text = convertView.findViewById(android.R.id.text1);
            text.setText(playlistNames.get(position));
            return convertView;
        }
    }
}
