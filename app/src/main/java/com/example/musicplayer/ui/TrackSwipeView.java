package com.example.musicplayer.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;

import com.example.musicplayer.QueueManager;
import com.example.musicplayer.R;
import com.example.musicplayer.Track;

import java.util.ArrayList;
import java.util.List;

public class TrackSwipeView extends FrameLayout {

    private static final int SNAP_VELOCITY = 800;
    private static final int PEEK_WIDTH_DP = 80; // visible part of adjacent tracks

    private Scroller scroller;
    private VelocityTracker velocityTracker;
    private float lastTouchX;
    private int centerIndex = 0; // index in the queue
    private List<Track> queue;
    private View[] childViews = new View[3]; // left, center, right
    private int pageWidth;
    private int peekWidth;
    private OnTrackChangeListener listener;

    private TextView centerTitle, centerArtist;
    private ImageView centerArt;
    private TextView leftTitle, leftArtist;
    private ImageView leftArt;
    private TextView rightTitle, rightArtist;
    private ImageView rightArt;

    public interface OnTrackChangeListener {
        void onTrackChangeRequest(int newIndex);
    }

    public TrackSwipeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scroller = new Scroller(context);
        inflate(context, R.layout.track_swipe_view, this);
        // Find child views
        leftArt = findViewById(R.id.left_art);
        leftTitle = findViewById(R.id.left_title);
        leftArtist = findViewById(R.id.left_artist);
        centerArt = findViewById(R.id.center_art);
        centerTitle = findViewById(R.id.center_title);
        centerArtist = findViewById(R.id.center_artist);
        rightArt = findViewById(R.id.right_art);
        rightTitle = findViewById(R.id.right_title);
        rightArtist = findViewById(R.id.right_artist);
    }

    public void setOnTrackChangeListener(OnTrackChangeListener l) {
        this.listener = l;
    }

    public void setQueue(List<Track> queue, int startIndex) {
        this.queue = queue;
        this.centerIndex = startIndex;
        updateViews();
        snapToCenter(false);
    }

    public void goToTrack(int index, boolean animate) {
        if (queue == null || index < 0 || index >= queue.size()) return;
        centerIndex = index;
        updateViews();
        snapToCenter(animate);
    }

    private void updateViews() {
        if (queue == null) return;
        setTrackOnView(queue, centerIndex, centerArt, centerTitle, centerArtist);
        if (centerIndex > 0) {
            setTrackOnView(queue, centerIndex - 1, leftArt, leftTitle, leftArtist);
            leftArt.setVisibility(View.VISIBLE);
        } else {
            leftArt.setVisibility(View.INVISIBLE);
        }
        if (centerIndex < queue.size() - 1) {
            setTrackOnView(queue, centerIndex + 1, rightArt, rightTitle, rightArtist);
            rightArt.setVisibility(View.VISIBLE);
        } else {
            rightArt.setVisibility(View.INVISIBLE);
        }
    }

    private void setTrackOnView(List<Track> queue, int index, ImageView art, TextView title, TextView artist) {
        Track t = queue.get(index);
        title.setText(t.getTitle());
        artist.setText(t.getArtist());
        // Load album art async
        loadArtAsync(art, t.getPath());
    }

    private void loadArtAsync(final ImageView view, final String path) {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                try {
                    mmr.setDataSource(path);
                    byte[] data = mmr.getEmbeddedPicture();
                    if (data != null) return BitmapFactory.decodeByteArray(data, 0, data.length);
                } catch (Exception e) {} finally {
                    try {
                    mmr.release();
                    } catch (Exception e) {}
                }
                return null;
            }
            @Override
            protected void onPostExecute(Bitmap bmp) {
                if (bmp != null) view.setImageBitmap(bmp);
                else view.setImageResource(R.drawable.default_art);
            }
        }.execute();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        pageWidth = getWidth();
        peekWidth = (int) (PEEK_WIDTH_DP * getResources().getDisplayMetrics().density);
        // Layout child views: center takes full width, left/right offset
        View leftPanel = findViewById(R.id.left_panel);
        View centerPanel = findViewById(R.id.center_panel);
        View rightPanel = findViewById(R.id.right_panel);
        leftPanel.layout(-pageWidth, 0, 0, getHeight());
        centerPanel.layout(0, 0, pageWidth, getHeight());
        rightPanel.layout(pageWidth, 0, 2 * pageWidth, getHeight());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - lastTouchX);
                if (dx > 10) return true;
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished()) scroller.abortAnimation();
                lastTouchX = event.getX();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastTouchX;
                // Scroll all panels together
                int newScroll = getScrollX() - (int) dx;
                // Clamp
                if (newScroll > peekWidth) newScroll = peekWidth;
                if (newScroll < -peekWidth) newScroll = -peekWidth;
                scrollTo(newScroll, 0);
                lastTouchX = event.getX();
                // Fade and scale based on offset
                updatePanelEffects(newScroll);
                break;
            case MotionEvent.ACTION_UP:
                velocityTracker.computeCurrentVelocity(1000);
                int velocityX = (int) velocityTracker.getXVelocity();
                int targetScroll = 0;
                if (Math.abs(velocityX) > SNAP_VELOCITY || Math.abs(getScrollX()) > pageWidth / 3) {
                    // commit to next/prev
                    if (getScrollX() > 0 && centerIndex > 0) {
                        targetScroll = pageWidth;
                    } else if (getScrollX() < 0 && centerIndex < queue.size() - 1) {
                        targetScroll = -pageWidth;
                    }
                }
                if (targetScroll != 0) {
                    final int dir = targetScroll > 0 ? -1 : 1; // -1 = prev, 1 = next
                    final int newIndex = centerIndex + (dir > 0 ? 1 : -1);
                    if (listener != null) listener.onTrackChangeRequest(newIndex);
                    // Animate to the new page
                    scroller.startScroll(getScrollX(), 0, targetScroll - getScrollX(), 0, 300);
                    invalidate();
                    // After scroll completes, update centerIndex
                    postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                centerIndex = newIndex;
                                updateViews();
                                snapToCenter(false);
                            }
                        }, 350);
                } else {
                    snapToCenter(true);
                }
                velocityTracker.recycle();
                velocityTracker = null;
                break;
        }
        return true;
    }

    private void updatePanelEffects(int scrollX) {
        float percent = Math.abs(scrollX) / (float) pageWidth;
        // Fade center out, fade side in, scale down center
        View center = findViewById(R.id.center_panel);
        View side;
        if (scrollX > 0) side = findViewById(R.id.left_panel);
        else side = findViewById(R.id.right_panel);

        center.setAlpha(1 - percent);
        center.setScaleX(1 - percent * 0.2f);
        center.setScaleY(1 - percent * 0.2f);

        side.setAlpha(percent);
        side.setScaleX(0.8f + percent * 0.2f);
        side.setScaleY(0.8f + percent * 0.2f);
    }

    private void snapToCenter(boolean animate) {
        if (animate) {
            scroller.startScroll(getScrollX(), 0, -getScrollX(), 0, 250);
            invalidate();
        } else {
            scrollTo(0, 0);
            updatePanelEffects(0);
        }
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            updatePanelEffects(getScrollX());
            postInvalidate();
        }
    }
}
