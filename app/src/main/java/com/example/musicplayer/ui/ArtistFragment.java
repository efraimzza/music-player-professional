
package com.example.musicplayer.ui;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.example.musicplayer.MediaDataManager;
import com.example.musicplayer.R;
import java.util.ArrayList;
import java.util.List;
import com.example.musicplayer.LogUtil;

public class ArtistFragment extends Fragment {
    private CustomListView listView;
    private customAdapter adapter;
    private List<String> albums = new ArrayList<>();
    View root;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_list, container, false);
        listView = root.findViewById(R.id.list);
        listView.setCustomEmptyView((TextView)root.findViewById(R.id.empty));
        adapter = new customAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String album = albums.get(position);
                    // Start a new fragment showing tracks of that album
                    Fragment trackFrag = new TrackListFragment();
                    Bundle args = new Bundle();
                    args.putString("type", "artist");
                    args.putString("value", album);
                    trackFrag.setArguments(args);
                    ((MainActivity) getActivity()).switchFragment(trackFrag, "tracks_album", true);
                }
            });

        loadData();
        return root;
    }

    private void loadData() {
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
                if(getActivity()!=null){
                    MediaDataManager mgr= ((MainActivity) getActivity()).getMgr();
                return mgr.getArtists();
                }
                return new ArrayList<>();
            }
            @Override
            protected void onPostExecute(List<String> result) {
                try{
                 /*   if(result.size()>0){
                        root.findViewById(R.id.empty).setVisibility(View.GONE);
                    }*/
                albums = result;
                adapter.setItems(getActivity(),albums);
                listView.refresh();
                }catch(Throwable e){LogUtil.logToFile(e);}
            }
        }.execute();
    }
    /*
     class AlbumAdapter extends BaseAdapter {
     @Override public int getCount() { return albums.size(); }
     @Override public Object getItem(int pos) { return albums.get(pos); }
     @Override public long getItemId(int pos) { return pos; }
     @Override
     public View getView(int position, View convertView, ViewGroup parent) {
     if (convertView == null) {
     convertView = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
     }
     TextView text = convertView.findViewById(android.R.id.text1);
     text.setText(albums.get(position));
     return convertView;
     }
     }*/
}

