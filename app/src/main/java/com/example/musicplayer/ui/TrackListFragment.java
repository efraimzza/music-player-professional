package com.example.musicplayer.ui;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.example.musicplayer.MediaDataManager;
import com.example.musicplayer.QueueManager;
import com.example.musicplayer.R;
import com.example.musicplayer.Track;
import java.util.ArrayList;
import java.util.List;
import com.example.musicplayer.service.PlaybackService;
import com.example.musicplayer.LogUtil;
import java.util.Collections;
import java.util.Comparator;
import android.media.MediaMetadataRetriever;
import android.content.Intent;

public class TrackListFragment extends Fragment {
    private CustomListView listView;
    private TrackAdapter adapter;
    private List<Track> tracks = new ArrayList<>();
    private String type, value;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_list, container, false);
        listView = root.findViewById(R.id.list);
        listView.setCustomEmptyView((TextView)root.findViewById(R.id.empty));
        adapter = new TrackAdapter();
        listView.setAdapter(adapter);
        Bundle args = getArguments();
        if (args != null) {
            type = args.getString("type");
            value = args.getString("value");
        }
        loadTracks();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Track track = tracks.get(position);
                    // Play all tracks of this group starting from clicked one
                    List<Track> queue = new ArrayList<>(tracks);
                    QueueManager.getInstance().setQueue(queue, position);
                    //PlaybackService svc = ((MainActivity) getActivity()).getService();
                    //if (svc != null) svc.initTrack(track.getPath());
                    ((MainActivity) getActivity()).startService(new Intent(getActivity(),PlaybackService.class).setAction(PlaybackService. ACTION_INIT_TRACK).putExtra("path",track.getPath()));
                }
            });
        return root;
    }

    private void loadTracks() {
        new AsyncTask<Void, Void, List<Track>>() {
            @Override
            protected List<Track> doInBackground(Void... params) {
                try{
                    if(getActivity()!=null){
                        MediaDataManager mgr= ((MainActivity) getActivity()).getMgr();
                        if ("album".equals(type)) return mgr.getTracksByAlbum(value);
                        else if ("artist".equals(type)) return mgr.getTracksByArtist(value);
                        else if ("genre".equals(type)) return mgr.getTracksByGenre(value);
                    }
                }catch(Throwable e){LogUtil.logToFile(e);}
                return new ArrayList<>();
            }
            @Override
            protected void onPostExecute(List<Track> result) {
                Collections.sort(result, new Comparator<Track>() {
                        @Override
                        public int compare(Track item1, Track item2) {
                            //LogUtil.logToFile("1="+extractDiscNum(item1.getPath())+" 2="+extractDiscNum(item2.getPath()));
                            //LogUtil.logToFile("logics="+extractDiscNumStr(item1.getPath()).compareToIgnoreCase(extractDiscNumStr(item2.getPath())));
                            //LogUtil.logToFile("logicmath="+Math.min(extractDiscNum(item1.getPath()),extractDiscNum(item2.getPath())));
                            //return extractDiscNum(item1.getPath())-extractDiscNum(item2.getPath());
                            //LogUtil.logToFile(item1.getCdTrackMumber()+"2="+item2.getCdTrackMumber());
                            return item1.getCdTrackMumber()-item2.getCdTrackMumber();
                            //return extractDiscNum(item1.getPath()).compareToIgnoreCase(extractDiscNum(item2.getPath()));
                        }
                    });
                tracks = result;
                listView.refresh();
            }
        }.execute();
    }
    public static int extractDiscNum(String filePath) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(filePath);
            String intstr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            intstr=intstr.replace("/","");
            if(intstr!=null) return Integer.parseInt(intstr);
            return 0;
        } catch (Exception e) {
            LogUtil.logToFile(e);
            return 0;
        } finally {
            try{
                mmr.release();
            }catch(Exception e){}
        }
    }
    public static String extractDiscNumStr(String filePath) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(filePath);
            String intstr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            if(intstr!=null) return intstr;
            return "0";
        } catch (Exception e) {
            return "0";
        } finally {
            try{
                mmr.release();
            }catch(Exception e){}
        }
    }
    class TrackAdapter extends BaseAdapter {
        @Override public int getCount() { return tracks.size(); }
        @Override public Object getItem(int pos) { return tracks.get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.item_track, parent, false);
            }
            Track t = tracks.get(position);
            TextView title = convertView.findViewById(R.id.track_title);
            TextView artist = convertView.findViewById(R.id.track_artist);
            title.setText(t.getTitle());
            artist.setText(t.getArtist());
            return convertView;
        }
    }
}
