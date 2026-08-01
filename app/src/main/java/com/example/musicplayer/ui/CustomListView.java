package com.example.musicplayer.ui;

import android.widget.ListView;
import android.content.Context;
import android.widget.ListAdapter;
import android.view.View;
import android.util.AttributeSet;
import com.example.musicplayer.LogUtil;
import android.widget.TextView;
import android.widget.BaseAdapter;
import android.os.Handler;

public class CustomListView extends ListView {
    TextView emptyView=null;
    public CustomListView(Context context){
        super(context);
        //LogUtil.logToFile("i1");
    }
    public CustomListView(Context context,  AttributeSet attrs) {
        super(context, attrs);
        //LogUtil.logToFile("i2");
    }

    public CustomListView(Context context,  AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //LogUtil.logToFile("i3");
    }
    /*CustomListView(Context context,View emptyView){
        super(context);
        this.emptyView=emptyView;
    }*/
    @Override
    public void setAdapter(ListAdapter adapter) {
        try{
        super.setAdapter(adapter);
        }catch(Throwable t){LogUtil.logToFile(t);}
        refresh();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        refresh();
    }

    public void setCustomEmptyView(TextView emptyView) {
        this.emptyView=emptyView;
        refresh();
    }
    
    public void refresh(){
        if(getAdapter()!=null){
            if(getAdapter() instanceof BaseAdapter){
                ((BaseAdapter)getAdapter()).notifyDataSetChanged();
            }
        }
        if(emptyView!=null){
            if(getAdapter()!=null){
            if(getAdapter().getCount()>0){
                emptyView.setVisibility(GONE);
            }else if(MainActivity.scanning){
                //LogUtil.logToFile("3");
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText("scanning="+MainActivity.curScanDir);
                new Handler().postDelayed(new Runnable(){public void run(){refresh();}},500);
            }else{
                //LogUtil.logToFile("4");
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText("not found");
            }
            }
        }
    }
}
