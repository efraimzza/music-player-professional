package com.example.musicplayer.ui;
import android.widget.BaseAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.List;
import java.util.ArrayList;
import android.app.Activity;
import android.graphics.Color;

public class customAdapter extends BaseAdapter {
    private List<String> items = new ArrayList<>();
    Activity activity;
    private int normalColor = Color.DKGRAY;
    private int pressedColor = Color.GRAY;
    public void setItems(Activity activity,List<String> items){
        this.activity=activity;
        this.items=items;
    }
    @Override public int getCount() { return items.size(); }
    @Override public Object getItem(int pos) { return items.get(pos); }
    @Override public long getItemId(int pos) { return pos; }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = activity.getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
        }
        // In getView after convertView is ready:
        if (convertView.getAnimation() == null) {
            convertView.setAlpha(0f);
            convertView.animate()
                .alpha(1f)
                .setDuration(200)
                //.setStartDelay(position * 50) // stagger
                .start();
        }
        TextView text = convertView.findViewById(android.R.id.text1);
        text.setText(items.get(position));
        
        RuntimeBackground.apply(
            activity,
            convertView,
            normalColor,
            pressedColor,
            28f,
            32f
        );
        
        return convertView;
    }
}
