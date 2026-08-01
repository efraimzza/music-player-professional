package com.example.musicplayer.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.example.musicplayer.QueueManager;
import com.example.musicplayer.R;
import com.example.musicplayer.Track;
import android.widget.AdapterView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.util.List;
import com.example.musicplayer.service.PlaybackService;
import android.content.Intent;

public class QueueFragment extends Fragment {
    private ListView listView;
    private QueueAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_queue, container, false);
        listView = root.findViewById(R.id.queue_list);
        adapter = new QueueAdapter();
        listView.setAdapter(adapter);
        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    QueueManager qm=QueueManager.getInstance();
                    qm.setCurrentIndex(position);
                    //PlaybackService svc = ((MainActivity) getActivity()).getService();
                    //if (svc != null) svc.initTrack(qm.getQueue().get(position).getPath());
                    ((MainActivity) getActivity()).startService(new Intent(getActivity(),PlaybackService.class).setAction(PlaybackService. ACTION_INIT_TRACK).putExtra("path",qm.getQueue().get(position).getPath()));
                }
            });
        
        // In onCreateView, after adapter setup:
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    // Start drag by moving item visually (we'll use a simple "move up/down" dialog)
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Move track");
                    String[] options = {"Move up", "Move down", "Remove"};
                    builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                QueueManager qm = QueueManager.getInstance();
                                List<Track> queue = qm.getQueue();
                                int curIdx = qm.getCurrentIndex();
                                if (which == 0 && position > 0) {
                                    Track removed = queue.remove(position);
                                    queue.add(position - 1, removed);
                                    adapter.notifyDataSetChanged();
                                } else if (which == 1 && position < queue.size() - 1) {
                                    Track removed = queue.remove(position);
                                    queue.add(position + 1, removed);
                                    adapter.notifyDataSetChanged();
                                } else if (which == 2) {
                                    queue.remove(position);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        });
                    builder.show();
                    return true;
                }
            });
        // Drag-to-reorder omitted for brevity (implement with OnTouchListener)
        return root;
    }

    class QueueAdapter extends BaseAdapter {
        @Override public int getCount() { return QueueManager.getInstance().getQueue().size(); }
        @Override public Object getItem(int pos) { return QueueManager.getInstance().getQueue().get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.item_track, parent, false);
            }
            Track track = QueueManager.getInstance().getQueue().get(position);
            TextView title = convertView.findViewById(R.id.track_title);
            TextView artist = convertView.findViewById(R.id.track_artist);
            title.setText(track.getTitle());
            artist.setText(track.getArtist());
            return convertView;
        }
    }
}
