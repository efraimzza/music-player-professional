package com.example.musicplayer.ui;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.musicplayer.QueueManager;
import com.example.musicplayer.R;
import com.example.musicplayer.Track;
import com.example.musicplayer.service.PlaybackService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerFragment extends Fragment {
    private ImageView albumArt;
    private TextView titleText, artistText, albumText;
    private SeekBar seekBar;
    private ImageButton btnPrev, btnPlayPause, btnNext, btnShuffle, btnRepeat;
    private PlaybackService service;

    private View mainView;
    private View trackInfoContentView;
    private View textsContentView;

    private VerticalSwipeLayout swipeLayoutImage;
    private VerticalSwipeLayout swipeLayoutTexts;
    private boolean isLandscape;

    // --- LruCache & Preload Executor ---
    private static LruCache<String, Bitmap> memoryCache;
    private final ExecutorService preloadExecutor = Executors.newSingleThreadExecutor();

    static {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8; // 1/8 מהזיכרון מוקצה למטמון
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int position = intent.getIntExtra("position", 0);
            int duration = intent.getIntExtra("duration", 1);
            if (seekBar != null) {
                seekBar.setMax(duration);
                seekBar.setProgress(position);
            }
        }
    };

    private BroadcastReceiver uiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        mainView = inflater.inflate(R.layout.fragment_player, container, false);

        if (isLandscape) {
            swipeLayoutImage = new VerticalSwipeLayout(getActivity(), R.layout.track_info_view, true);
            trackInfoContentView = inflater.inflate(R.layout.track_info_view, swipeLayoutImage, false);
            swipeLayoutImage.addView(trackInfoContentView);
            swipeLayoutImage.setMainView(trackInfoContentView);

            FrameLayout imageContainer = mainView.findViewById(R.id.swipe_container);
            if (imageContainer != null) imageContainer.addView(swipeLayoutImage);

            swipeLayoutTexts = new VerticalSwipeLayout(getActivity(), R.layout.texts_only_view, false);
            textsContentView = inflater.inflate(R.layout.texts_only_view, swipeLayoutTexts, false);
            swipeLayoutTexts.addView(textsContentView);
            swipeLayoutTexts.setMainView(textsContentView);

            FrameLayout textsContainer = mainView.findViewById(R.id.texts_swipe_container);
            if (textsContainer != null) textsContainer.addView(swipeLayoutTexts);

            swipeLayoutImage.setSibling(swipeLayoutTexts);
            swipeLayoutTexts.setSibling(swipeLayoutImage);

            albumArt = trackInfoContentView.findViewById(R.id.album_art);
            titleText = textsContentView.findViewById(R.id.track_title);
            artistText = textsContentView.findViewById(R.id.artist_name);
            albumText = textsContentView.findViewById(R.id.album_name);

        } else {
            swipeLayoutImage = new VerticalSwipeLayout(getActivity(), R.layout.track_info_view, true);
            trackInfoContentView = inflater.inflate(R.layout.track_info_view, swipeLayoutImage, false);
            swipeLayoutImage.addView(trackInfoContentView);
            swipeLayoutImage.setMainView(trackInfoContentView);

            LinearLayout trackInfoContainer = mainView.findViewById(R.id.track_info);
            if (trackInfoContainer != null) trackInfoContainer.addView(swipeLayoutImage);

            albumArt = trackInfoContentView.findViewById(R.id.album_art);
            titleText = trackInfoContentView.findViewById(R.id.track_title);
            artistText = trackInfoContentView.findViewById(R.id.artist_name);
            albumText = trackInfoContentView.findViewById(R.id.album_name);
        }

        seekBar = mainView.findViewById(R.id.seek_bar);
        btnPrev = mainView.findViewById(R.id.btn_prev);
        btnPlayPause = mainView.findViewById(R.id.btn_play_pause);
        btnNext = mainView.findViewById(R.id.btn_next);
        btnShuffle = mainView.findViewById(R.id.btn_shuffle);
        btnRepeat = mainView.findViewById(R.id.btn_repeat);

        setupControllerListeners();
        updateControllerButtonsState();
        updateUI();

        return mainView;
    }

    private void setupControllerListeners() {
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && service != null) service.seekTo(progress);
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });
        }

        if (btnPlayPause != null) {
            btnPlayPause.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {if (service != null) service.playPause(); }});
        }
        if (btnPrev != null) {
            btnPrev.setOnClickListener(new View.OnClickListener() {
                                               @Override
                                               public void onClick(View v) { if (service != null) service.previous(); }});
        }
        if (btnNext != null) {
            btnNext.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v)  { if (service != null) service.next(); }});
        }
        if (btnShuffle != null) {
            btnShuffle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                QueueManager.getInstance().toggleShuffle();
                updateShuffleButton();
            }});
        }
        if (btnRepeat != null) {
            btnRepeat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                QueueManager qm = QueueManager.getInstance();
                qm.setRepeatMode((qm.getRepeatMode() + 1) % 3);
                updateRepeatButton();
            }});
        }
    }

    private void updateControllerButtonsState() {
        updateShuffleButton();
        updateRepeatButton();
    }

    private void updateShuffleButton() {
        if (btnShuffle != null) {
            boolean shuff = QueueManager.getInstance().isShuffle();
            //btnShuffle.setColorFilter(shuff ? Color.RED : Color.TRANSPARENT);
            btnShuffle.setImageResource(shuff ? R.drawable.ic_shuffle_off : R.drawable.ic_shuffle);
        }
    }

    private void updateRepeatButton() {
        if (btnRepeat != null) {
            int mode = QueueManager.getInstance().getRepeatMode();
            switch (mode) {
                //case QueueManager.REPEAT_OFF: btnRepeat.setColorFilter(Color.TRANSPARENT); break;
                //case QueueManager.REPEAT_ALL: btnRepeat.setColorFilter(Color.GREEN); break;
                //case QueueManager.REPEAT_ONE: btnRepeat.setColorFilter(Color.RED); break;
                case QueueManager.REPEAT_OFF: btnRepeat.setImageResource(R.drawable.ic_next_song); break;
                case QueueManager.REPEAT_ALL: btnRepeat.setImageResource(R.drawable.ic_repeat_queue); break;
                case QueueManager.REPEAT_ONE: btnRepeat.setImageResource(R.drawable.ic_repeat_song); break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(progressReceiver, new IntentFilter("com.example.musicplayer.PROGRESS_UPDATE"));
        getActivity().registerReceiver(uiReceiver, new IntentFilter("com.example.musicplayer.UI_UPDATE"));

        service = PlaybackService.getInstance();
        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            getActivity().unregisterReceiver(progressReceiver);
            getActivity().unregisterReceiver(uiReceiver);
        } catch (Exception ignored) {}
    }

    private void updateUI() {
        if (getActivity() == null) return;
        service = PlaybackService.getInstance();
        if (service == null) return;

        Track track = QueueManager.getInstance().getCurrentTrack();
        if (track != null) {
            if (titleText != null) titleText.setText(track.getTitle());
            if (artistText != null) artistText.setText(track.getArtist());
            if (albumText != null) albumText.setText(track.getAlbum());
            if (albumArt != null) {
                Bitmap art = getAlbumArt(track.getPath());
                if (art != null) albumArt.setImageBitmap(art);
                else albumArt.setImageResource(R.drawable.default_art);
            }

            // טעינה מראש של השיר הבא והשיר הקודם ב-Thread נפרד ברקע!
            preloadNeighborTracks();
        }

        if (seekBar != null) {
            seekBar.setMax(service.getDuration());
            seekBar.setProgress(service.getCurrentPosition());
        }
        if (btnPlayPause != null) {
            btnPlayPause.setImageResource(service.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    // --- טעינה ברקע עבור השירים השכנים ---
    private void preloadNeighborTracks() {
        preloadExecutor.execute(new Runnable() {public void run(){
            Track prevTrack = getTrackOffset(-1);
            Track nextTrack = getTrackOffset(1);
            if (prevTrack != null) getAlbumArt(prevTrack.getPath());
            if (nextTrack != null) getAlbumArt(nextTrack.getPath());
        }});
    }

    // --- טעינת תמונות יעילה עם LruCache ו-inSampleSize ---
    private Bitmap getAlbumArt(String filePath) {
        if (filePath == null) return null;

        Bitmap cached = memoryCache.get(filePath);
        if (cached != null) return cached;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Bitmap bitmap = null;
        try {
            retriever.setDataSource(filePath);
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(art, 0, art.length, options);

                options.inSampleSize = calculateInSampleSize(options, 500, 500);
                options.inJustDecodeBounds = false;

                bitmap = BitmapFactory.decodeByteArray(art, 0, art.length, options);
                if (bitmap != null) {
                    memoryCache.put(filePath, bitmap);
                }
            }
        } catch (Exception ignored) {
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
        return bitmap;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Track getTrackOffset(int offset) {
        QueueManager qm = QueueManager.getInstance();
        List<Track> queue = qm.getQueue();
        if (queue == null || queue.isEmpty()) return null;
        int curr = qm.getCurrentIndex();
        int target = curr + offset;
        if (target < 0 || target >= queue.size()) return null;
        return queue.get(target);
    }

    private View createMockViewForTrack(Track track, int layoutResId, ViewGroup parent) {
        View mock = getActivity().getLayoutInflater().inflate(layoutResId, parent, false);
        populateViewWithTrackData(mock, track);
        return mock;
    }

    private void populateViewWithTrackData(View view, Track track) {
        ImageView mArt = view.findViewById(R.id.album_art);
        TextView mTitle = view.findViewById(R.id.track_title);
        TextView mArtist = view.findViewById(R.id.artist_name);
        TextView mAlbum = view.findViewById(R.id.album_name);

        if (mTitle != null) mTitle.setText(track.getTitle());
        if (mArtist != null) mArtist.setText(track.getArtist());
        if (mAlbum != null) mAlbum.setText(track.getAlbum());

        if (mArt != null) {
            Bitmap art = getAlbumArt(track.getPath());
            if (art != null) mArt.setImageBitmap(art);
            else mArt.setImageResource(R.drawable.default_art);
        }
    }

    // ==========================================
    // מחלקה פנימית לניהול החלקות (Vertical Swipe)
    // ==========================================
    private class VerticalSwipeLayout extends FrameLayout {
        private float startY;
        private boolean isDragging = false;
        private View currentMainView;
        private View mockView; 
        private boolean isSwipingUp; 
        private VerticalSwipeLayout siblingLayout;
        private int contentLayoutResId;
        private boolean isMaster;

        public VerticalSwipeLayout(Context context, int contentLayoutResId, boolean isMaster) {
            super(context);
            this.contentLayoutResId = contentLayoutResId;
            this.isMaster = isMaster;
        }

        public void setMainView(View view) {
            this.currentMainView = view;
        }

        public void setSibling(VerticalSwipeLayout sibling) {
            this.siblingLayout = sibling;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = ev.getRawY();
                    isDragging = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dy = ev.getRawY() - startY;
                    if (Math.abs(dy) > 30) {
                        isDragging = true;
                        return true;
                    }
                    break;
            }
            return super.onInterceptTouchEvent(ev);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (service == null) return false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    float dy = event.getRawY() - startY;

                    if (!isDragging && Math.abs(dy) > 30) {
                        isDragging = true;
                    }

                    if (isDragging) {
                        float viewHeight = getHeight();
                        float fraction = Math.abs(dy) / viewHeight;
                        fraction = Math.max(0f, Math.min(1f, fraction));

                        if (mockView == null) {
                            isSwipingUp = dy < 0; 
                            Track mockTrack = getTrackOffset(isSwipingUp ? 1 : -1);

                            if (mockTrack != null) {
                                mockView = createMockViewForTrack(mockTrack, contentLayoutResId, this);
                                addView(mockView);

                                if (siblingLayout != null) {
                                    siblingLayout.createMockForSibling(mockTrack);
                                }
                            } else {
                                isDragging = false;
                                break;
                            }
                        }

                        applyAnimation(dy, fraction);
                        if (siblingLayout != null) {
                            siblingLayout.applyAnimation(dy, fraction);
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isDragging && mockView != null) {
                        float finalDy = event.getRawY() - startY;
                        float viewHeight = getHeight();

                        // סף 25% להחלפה
                        final boolean shouldSwitch = Math.abs(finalDy) > viewHeight * 0.25f;

                        // האנימציות ירוצו עד הסוף, ורק ב-EndAction תתבצע החלפת השיר!
                        animateToTarget(shouldSwitch, isSwipingUp, viewHeight);

                        if (siblingLayout != null) {
                            siblingLayout.animateToTarget(shouldSwitch, isSwipingUp, siblingLayout.getHeight());
                        }

                        isDragging = false;
                    }
                    break;
            }
            return true;
        }

        public void createMockForSibling(Track track) {
            mockView = createMockViewForTrack(track, contentLayoutResId, this);
            addView(mockView);
        }

        public void applyAnimation(float dy, float fraction) {
            if (currentMainView != null) {
                float mainScale = 1.0f - (0.2f * fraction);
                float mainAlpha = 1.0f - fraction;
                currentMainView.setTranslationY(dy);
                currentMainView.setScaleX(mainScale);
                currentMainView.setScaleY(mainScale);
                currentMainView.setAlpha(mainAlpha);
            }

            if (mockView != null) {
                float viewHeight = getHeight();
                boolean dynamicSwipingUp = dy < 0;
                float mockStartY = dynamicSwipingUp ? viewHeight : -viewHeight;
                float mockScale = 0.8f + (0.2f * fraction);
                float mockAlpha = fraction;
                mockView.setTranslationY(mockStartY + dy);
                mockView.setScaleX(mockScale);
                mockView.setScaleY(mockScale);
                mockView.setAlpha(mockAlpha);
            }
        }

        public void animateToTarget(final boolean shouldSwitch, final boolean isSwipingUp, float viewHeight) {
            float targetMainY = shouldSwitch ? (isSwipingUp ? -viewHeight : viewHeight) : 0;
            float targetMockY = shouldSwitch ? 0 : (isSwipingUp ? viewHeight : -viewHeight);

            float targetMainScale = shouldSwitch ? 0.8f : 1.0f;
            float targetMainAlpha = shouldSwitch ? 0f : 1.0f;

            float targetMockScale = shouldSwitch ? 1.0f : 0.8f;
            float targetMockAlpha = shouldSwitch ? 1.0f : 0f;

            // 1. אנימציה ל-View הראשי
            if (currentMainView != null) {
                currentMainView.animate()
                    .translationY(targetMainY)
                    .scaleX(targetMainScale).scaleY(targetMainScale)
                    .alpha(targetMainAlpha)
                    .setDuration(220)
                    .withLayer()
                    .start();
            }

            // 2. אנימציה ל-Mock View (החדש)
            if (mockView != null) {
                mockView.animate()
                    .translationY(targetMockY)
                    .scaleX(targetMockScale).scaleY(targetMockScale)
                    .alpha(targetMockAlpha)
                    .setDuration(220)
                    .withLayer()
                    .withEndAction(new Runnable() {public void run(){
                    // בסיום האנימציה - אם אושר מעבר שיר, ורק ה-Master מבצע את שינוי השיר
                    if (shouldSwitch) {
                        if (isMaster) {
                            if (isSwipingUp) service.next();
                            else service.previous();

                            updateUI();
                        }
                    }

                    // איפוס ה-View הראשי והסרת ה-Mock בלבד
                    if (currentMainView != null) {
                        currentMainView.setTranslationY(0);
                        currentMainView.setScaleX(1f);
                        currentMainView.setScaleY(1f);
                        currentMainView.setAlpha(1f);
                    }

                    removeView(mockView);
                    mockView = null;
                }}).start();
            }
        }
    }
}
