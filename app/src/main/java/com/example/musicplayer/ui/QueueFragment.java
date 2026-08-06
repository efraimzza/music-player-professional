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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import com.example.musicplayer.LogUtil;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.musicplayer.QueueManager;
import com.example.musicplayer.R;
import com.example.musicplayer.Track;
import com.example.musicplayer.service.PlaybackService;

import java.util.IdentityHashMap;
import java.util.List;
import android.os.Build;
import android.os.SystemClock;

public class QueueFragment extends Fragment {
    private ListView listView;
    private QueueAdapter adapter;

    private FrameLayout dragLayer;
    private ImageView floatingView;
    private Bitmap floatingBitmap;

    private final Handler handler =
    new Handler(Looper.getMainLooper());

    private Runnable longPressRunnable;

    private boolean dragging = false;

    private int pendingPosition = AdapterView.INVALID_POSITION;
    private int dragPosition = AdapterView.INVALID_POSITION;

    private float downX;
    private float downY;
    private float downRawY;

    private int dragTouchOffset;
    private int touchSlop;
    private int lastTouchX;
    private int lastTouchY;
    private int lastRawY;

    private boolean autoScrolling = false;

    private final Runnable autoScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!dragging || listView == null) {
                autoScrolling = false;
                return;
            }

            int[] location = new int[2];
            listView.getLocationOnScreen(location);

            int top = location[1];
            int bottom = top + listView.getHeight();

            int edgeSize = dpToPixels(120);
            int scrollAmount = 0;

            if (lastRawY < top + edgeSize) {
                int distance = (top + edgeSize) - lastRawY;
                scrollAmount = -Math.min(35, Math.max(8, distance / 4));
            } else if (lastRawY > bottom - edgeSize) {
                int distance = lastRawY - (bottom - edgeSize);
                scrollAmount = Math.min(35, Math.max(8, distance / 4));
            }

            if (scrollAmount != 0) {
                /*
                 * scrollListBy הוא Android Framework API,
                 * ללא AndroidX וללא dependency.
                 */
                if (Build.VERSION.SDK_INT >= 19) {
                    listView.scrollListBy(scrollAmount);
                } else {
                    listView.smoothScrollBy(scrollAmount, 16);
                }

                /*
                 * חייבים לבדוק שוב את הפריט שמתחת לאצבע
                 * לאחר שהרשימה זזה.
                 */
                updateDragTarget();

                handler.postDelayed(this, 16);
            } else {
                autoScrolling = false;
            }
        }
    };



    private BroadcastReceiver queueReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (adapter != null) adapter.notifyDataSetChanged();
            LogUtil.logToFile("qunoti");
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(queueReceiver, new IntentFilter("UPDATE_QUEUE_UI"));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(queueReceiver);
    }


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View content = inflater.inflate(
            R.layout.fragment_queue,
            container,
            false
        );

        /*
         * שכבה שמאפשרת להציג את הפריט הצף מעל ה-ListView.
         * אין שימוש ב-WindowManager ולכן אין צורך ב-token או בהרשאות.
         */
        dragLayer = new FrameLayout(getActivity());
        dragLayer.addView(
            content,
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        );

        listView = content.findViewById(R.id.queue_list);
        adapter = new QueueAdapter();
        listView.setAdapter(adapter);

        touchSlop = ViewConfiguration
            .get(getActivity())
            .getScaledTouchSlop();

        listView.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent,
                                        View view,
                                        int position,
                                        long id) {

                    if (dragging) {
                        return;
                    }

                    QueueManager qm =
                        QueueManager.getInstance();

                    List<Track> queue = qm.getQueue();

                    if (position < 0 ||
                        position >= queue.size()) {
                        return;
                    }

                    qm.setCurrentIndex(position);

                    Intent intent = new Intent(
                        getActivity(),
                        PlaybackService.class
                    );

                    intent.setAction(
                        PlaybackService.ACTION_INIT_TRACK
                    );

                    intent.putExtra(
                        "path",
                        queue.get(position).getPath()
                    );

                    getActivity().startService(intent);
                }
            }
        );

        /*
         * גרירה ידנית באמצעות Touch בלבד.
         * לחיצה רגילה עדיין עובדת כרגיל.
         */
        listView.setOnTouchListener(
            new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v,
                                       MotionEvent event) {

                    int action =
                        event.getActionMasked();

                    if (action == MotionEvent.ACTION_DOWN) {
                        downX = event.getX();
                        downY = event.getY();
                        downRawY = event.getRawY();

                        pendingPosition =
                            listView.pointToPosition(
                            (int) downX,
                            (int) downY
                        );

                        if (pendingPosition !=
                            AdapterView.INVALID_POSITION) {

                            cancelLongPress();

                            longPressRunnable =
                                new Runnable() {
                                @Override
                                public void run() {
                                    if (!dragging &&
                                        pendingPosition !=
                                        AdapterView.INVALID_POSITION) {

                                        startDragging(
                                            pendingPosition,
                                            downRawY
                                        );
                                    }
                                }
                            };

                            handler.postDelayed(
                                longPressRunnable,
                                450
                            );
                        }

                        /*
                         * false מאפשר ל-ListView לטפל בגלילה
                         * ובלחיצה רגילה.
                         */
                        return false;
                    }

                    if (action == MotionEvent.ACTION_MOVE) {
                        lastTouchX = (int) event.getX();
                        lastTouchY = (int) event.getY();
                        lastRawY = (int) event.getRawY();

                        if (dragging) {
                            moveFloatingView(lastRawY);
                            updateDragTarget();

                            int[] location = new int[2];
                            listView.getLocationOnScreen(location);

                            int top = location[1];
                            int bottom = top + listView.getHeight();
                            int edgeSize = dpToPixels(120);

                            boolean nearTop =
                                lastRawY < top + edgeSize;

                            boolean nearBottom =
                                lastRawY > bottom - edgeSize;

                            if (nearTop || nearBottom) {
                                startAutoScrolling();
                            } else {
                                stopAutoScrolling();
                            }

                            return true;
                        }

                        float distanceX =
                            Math.abs(event.getX() - downX);

                        float distanceY =
                            Math.abs(event.getY() - downY);

                        if (distanceX > touchSlop ||
                            distanceY > touchSlop) {
                            cancelLongPress();
                        }

                        return false;
                    }




                    if (action == MotionEvent.ACTION_UP ||
                        action == MotionEvent.ACTION_CANCEL) {

                        cancelLongPress();

                        if (dragging) {
                            finishDragging();
                            return true;
                        }

                        pendingPosition =
                            AdapterView.INVALID_POSITION;

                        return false;
                    }

                    return dragging;
                }
            }
        );

        return dragLayer;
    }

    private void startDragging(int position,
                               float rawY) {

        int firstPosition =
            listView.getFirstVisiblePosition();

        int childIndex =
            position - firstPosition;

        if (childIndex < 0 ||
            childIndex >= listView.getChildCount()) {
            return;
        }

        View row =
            listView.getChildAt(childIndex);

        if (row == null) {
            return;
        }

        dragging = true;
        dragPosition = position;

        /*
         * ציור ידני של השורה ל-Bitmap.
         * לא משתמשים ב-drawing cache deprecated.
         */
        floatingBitmap = Bitmap.createBitmap(
            row.getWidth(),
            row.getHeight(),
            Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(floatingBitmap);
        row.draw(canvas);

        floatingView = new ImageView(getActivity());
        floatingView.setImageBitmap(floatingBitmap);
        floatingView.setScaleType(
            ImageView.ScaleType.FIT_XY
        );
        floatingView.setAlpha(0.85f);

        int[] rowLocation = new int[2];
        int[] layerLocation = new int[2];

        row.getLocationOnScreen(rowLocation);
        dragLayer.getLocationOnScreen(layerLocation);

        dragTouchOffset =
            (int) rawY - rowLocation[1];

        FrameLayout.LayoutParams params =
            new FrameLayout.LayoutParams(
            row.getWidth(),
            row.getHeight()
        );

        params.leftMargin =
            rowLocation[0] - layerLocation[0];

        params.topMargin =
            rowLocation[1] - layerLocation[1];

        dragLayer.addView(floatingView, params);
        
        /*
         * ביטול מצב pressed של הפריט המקורי ושל ה-ListView.
         * ה-Bitmap כבר נוצר קודם, ולכן רק הפריט הצף נשאר אפור.
         */
        row.setPressed(false);
        listView.setPressed(false);

        MotionEvent cancelEvent = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_CANCEL,
            0.0f,
            0.0f,
            0
        );

        listView.onTouchEvent(cancelEvent);
        cancelEvent.recycle();

        row.setPressed(false);
        listView.setPressed(false);

        row.refreshDrawableState();
        listView.refreshDrawableState();
        

        /*
         * משאיר placeholder בגובה המקורי.
         */
        row.setVisibility(View.INVISIBLE);

        listView.requestDisallowInterceptTouchEvent(true);
        adapter.notifyDataSetChanged();
    }

    private void moveFloatingView(float rawY) {
        if (floatingView == null) {
            return;
        }

        int[] layerLocation = new int[2];
        dragLayer.getLocationOnScreen(layerLocation);

        FrameLayout.LayoutParams params =
            (FrameLayout.LayoutParams)
            floatingView.getLayoutParams();

        params.topMargin =
            (int) rawY -
            layerLocation[1] -
            dragTouchOffset;

        floatingView.setLayoutParams(params);
    }

    private void moveItem(final int from,
                          final int to) {

        QueueManager qm =
            QueueManager.getInstance();

        List<Track> queue =
            qm.getQueue();

        if (from < 0 ||
            to < 0 ||
            from >= queue.size() ||
            to >= queue.size() ||
            from == to) {
            return;
        }

        /*
         * שומר מיקומי שורות לפני העדכון,
         * כדי להנפיש את השורות שזזות.
         */
        final IdentityHashMap<Track, Integer> oldTops =
            captureVisibleRowPositions(queue);

        Track movedTrack =
            queue.remove(from);

        queue.add(to, movedTrack);

        updateCurrentIndex(qm, from, to);

        dragPosition = to;
        adapter.notifyDataSetChanged();

        listView.post(
            new Runnable() {
                @Override
                public void run() {
                    animateVisibleRows(oldTops);
                }
            }
        );
    }

    private IdentityHashMap<Track, Integer>
    captureVisibleRowPositions(List<Track> queue) {

        IdentityHashMap<Track, Integer> positions =
            new IdentityHashMap<Track, Integer>();

        int firstPosition =
            listView.getFirstVisiblePosition();

        int childCount =
            listView.getChildCount();

        int i;

        for (i = 0; i < childCount; i++) {
            int position = firstPosition + i;

            if (position >= 0 &&
                position < queue.size()) {

                View child =
                    listView.getChildAt(i);

                if (child != null) {
                    positions.put(
                        queue.get(position),
                        child.getTop()
                    );
                }
            }
        }

        return positions;
    }

    private void animateVisibleRows(
        IdentityHashMap<Track, Integer> oldTops) {

        QueueManager qm =
            QueueManager.getInstance();

        List<Track> queue =
            qm.getQueue();

        int firstPosition =
            listView.getFirstVisiblePosition();

        int childCount =
            listView.getChildCount();

        int i;

        for (i = 0; i < childCount; i++) {
            int position = firstPosition + i;

            if (position < 0 ||
                position >= queue.size()) {
                continue;
            }

            View child =
                listView.getChildAt(i);

            if (child == null ||
                child.getVisibility() != View.VISIBLE) {
                continue;
            }

            Integer oldTop =
                oldTops.get(queue.get(position));

            if (oldTop == null) {
                continue;
            }

            int difference =
                oldTop - child.getTop();

            if (difference != 0) {
                TranslateAnimation animation =
                    new TranslateAnimation(
                    0,
                    0,
                    difference,
                    0
                );

                animation.setDuration(160);
                animation.setInterpolator(
                    new DecelerateInterpolator()
                );

                child.clearAnimation();
                child.startAnimation(animation);
            }
        }
    }

    private void updateCurrentIndex(QueueManager qm,
                                    int from,
                                    int to) {

        int currentIndex =
            qm.getCurrentIndex();

        if (currentIndex < 0) {
            return;
        }

        if (currentIndex == from) {
            qm.setCurrentIndex(to);
        } else if (from < currentIndex &&
                   to >= currentIndex) {

            qm.setCurrentIndex(currentIndex - 1);
        } else if (from > currentIndex &&
                   to <= currentIndex) {

            qm.setCurrentIndex(currentIndex + 1);
        }
    }



    private void cancelLongPress() {
        if (longPressRunnable != null) {
            handler.removeCallbacks(longPressRunnable);
            longPressRunnable = null;
        }
    }

    private void finishDragging() {
        stopAutoScrolling();

        cancelLongPress();

        if (floatingView != null) {
            dragLayer.removeView(floatingView);
            floatingView.setImageBitmap(null);
            floatingView = null;
        }

        floatingBitmap = null;

        dragging = false;
        dragPosition = AdapterView.INVALID_POSITION;
        pendingPosition = AdapterView.INVALID_POSITION;

        listView.requestDisallowInterceptTouchEvent(false);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        stopAutoScrolling();
        cancelLongPress();

        if (floatingView != null &&
            dragLayer != null) {
            dragLayer.removeView(floatingView);
        }

        floatingView = null;
        floatingBitmap = null;
        dragging = false;

        listView = null;
        adapter = null;
        dragLayer = null;

        super.onDestroyView();
    }
    private int dpToPixels(int dp) {
        float density = getResources()
            .getDisplayMetrics()
            .density;

        return (int) (dp * density + 0.5f);
    }

    private void startAutoScrolling() {
        if (!autoScrolling) {
            autoScrolling = true;
            handler.post(autoScrollRunnable);
        }
    }

    private void stopAutoScrolling() {
        autoScrolling = false;
        handler.removeCallbacks(autoScrollRunnable);
    }

    private void updateDragTarget() {
        if (!dragging) {
            return;
        }

        int targetPosition = listView.pointToPosition(
            lastTouchX,
            lastTouchY
        );

        if (targetPosition != AdapterView.INVALID_POSITION &&
            targetPosition != dragPosition) {

            moveItem(dragPosition, targetPosition);
        }
    }

    class QueueAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return QueueManager
                .getInstance()
                .getQueue()
                .size();
        }

        @Override
        public Object getItem(int position) {
            return QueueManager
                .getInstance()
                .getQueue()
                .get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position,
                            View convertView,
                            ViewGroup parent) {

            if (convertView == null) {
                convertView = getActivity()
                    .getLayoutInflater()
                    .inflate(
                    R.layout.item_track,
                    parent,
                    false
                );
            }
            convertView.setPressed(false);
            
            Track track =
                QueueManager
                .getInstance()
                .getQueue()
                .get(position);

            TextView title =
                convertView.findViewById(
                R.id.track_title
            );

            TextView artist =
                convertView.findViewById(
                R.id.track_artist
            );

            title.setText(track.getTitle());
            artist.setText(track.getArtist());

            if (dragging &&
                position == dragPosition) {
                convertView.setVisibility(
                    View.INVISIBLE
                );
            } else {
                convertView.setVisibility(
                    View.VISIBLE
                );
            }
            convertView.setPressed(false);
            return convertView;
        }
    }


    }
    
