package com.example.musicplayer.ui;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.example.musicplayer.MediaDataManager;
import com.example.musicplayer.R;
import com.example.musicplayer.Track;
import java.util.ArrayList;
import java.util.List;
import android.widget.AdapterView;
import java.io.File;
import com.example.musicplayer.service.PlaybackService;
import com.example.musicplayer.LogUtil;
import com.example.musicplayer.QueueManager;
import android.content.Intent;

public class SearchFragment extends Fragment {
    private EditText searchBox;
    private ListView resultsList;
    private SearchAdapter adapter;
    private List<Track> allTracks = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_search, container, false);
        searchBox = root.findViewById(R.id.search_box);
        resultsList = root.findViewById(R.id.results_list);
        adapter = new SearchAdapter();
        resultsList.setAdapter(adapter);
        resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Track track=allTracks.get(position);
                        try{
                            // Add to queue and play
                            
                            List<Track> list = new ArrayList<>();
                            list.add(track);
                            QueueManager.getInstance().setQueue(list, 0,getActivity());
                            //PlaybackService svc = ((MainActivity) getActivity()).getService();
                            //if (svc != null) svc.initTrack(track.getPath());
                            ((MainActivity) getActivity()).startService(new Intent(getActivity(),PlaybackService.class).setAction(PlaybackService. ACTION_INIT_TRACK).putExtra("path",track.getPath()));
                        }catch(Throwable e){LogUtil.logToFile(e);}
                    }
                
            });
        searchBox.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
                @Override public void afterTextChanged(Editable s) {}
            });

        loadAllTracks();
        return root;
    }

    private void loadAllTracks() {
        new AsyncTask<Void, Void, List<Track>>() {
            @Override
            protected List<Track> doInBackground(Void... params) {
                if(getActivity()!=null){
                    MediaDataManager mgr= ((MainActivity) getActivity()).getMgr();
                return mgr.getAllTracks();
                }
                return new ArrayList<>();
            }
            @Override
            protected void onPostExecute(List<Track> tracks) {
                allTracks = tracks;
                adapter.setData(allTracks);
            }
        }.execute();
    }

    private void filter(String query) {
        List<Track> filtered = new ArrayList<>();
        for (Track t : allTracks) {
            if (t.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                t.getArtist().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(t);
            }
        }
        adapter.setData(filtered);
    }

    class SearchAdapter extends BaseAdapter {
        private List<Track> data = new ArrayList<>();
        public void setData(List<Track> list) { data = list; notifyDataSetChanged(); }
        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int pos) { return data.get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.item_track, parent, false);
            }
            Track t = data.get(position);
            TextView title = convertView.findViewById(R.id.track_title);
            TextView artist = convertView.findViewById(R.id.track_artist);
            title.setText(t.getTitle());
            artist.setText(t.getArtist());
            return convertView;
        }
    }
}

