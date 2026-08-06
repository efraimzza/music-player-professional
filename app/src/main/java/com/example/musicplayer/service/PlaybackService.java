package com.example.musicplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

import com.example.musicplayer.LogUtil;
import com.example.musicplayer.QueueManager;
import com.example.musicplayer.R;
import com.example.musicplayer.Track;
import com.example.musicplayer.ui.MainActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaybackService extends Service implements
MediaPlayer.OnPreparedListener,
MediaPlayer.OnCompletionListener,
MediaPlayer.OnErrorListener,
AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_INIT_TRACK = "com.example.musicplayer.INIT_TRACK";
    public static final String ACTION_PLAY_PAUSE = "com.example.musicplayer.PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.example.musicplayer.NEXT";
    public static final String ACTION_PREV = "com.example.musicplayer.PREV";
    public static final String ACTION_STOP = "com.example.musicplayer.STOP";
    public static final String ACTION_CLOSE = "com.example.musicplayer.CLOSE"; // פעולת סגירה מלאה (X)
    public static final String ACTION_TOGGLE_REPEAT = "com.example.musicplayer.TOGGLE_REPEAT";
    public static final String ACTION_TOGGLE_SHUFFLE = "com.example.musicplayer.TOGGLE_SHUFFLE";

    private static final String CHANNEL_ID = "music_playback_channel";
    private static final int NOTIFICATION_ID = 1;

    // הגדרת הפעולות האפשריות בנוטיפיקציה להתאמה אישית
    public enum ActionType {
        PREV, PLAY_PAUSE, NEXT, SHUFFLE, REPEAT, STOP, CLOSE
        }

    // סידור הכפתורים: Play/Pause ממוקם במרכז (אינדקס 2)
    private ActionType[] notificationActionOrder = new ActionType[]{
        ActionType.SHUFFLE,
        ActionType.REPEAT,
        ActionType.PREV,
        ActionType.PLAY_PAUSE, // מרכז
        ActionType.NEXT,
        ActionType.CLOSE      // לחצן X לסגירה
    };

    // במצב מוקטן (Compact View) הוצג אינדקס 1, 2, 3 -> PREV, PLAY_PAUSE, NEXT (Play/Pause באמצע)
    private int[] compactViewIndices = new int[]{1, 2, 3};

    private MediaPlayer mediaPlayer;
    private MediaSession mediaSession;
    private AudioManager audioManager;
    private QueueManager queueManager;
    private final Handler handler = new Handler();
    private boolean isPlaying = false;
    private boolean isPreparing = false;
    private Runnable progressRunnable;
    private BroadcastReceiver notificationReceiver;
    private static PlaybackService instance = null;

    public class LocalBinder extends Binder {
        public PlaybackService getService() { return instance; }
    }

    private final IBinder binder = new LocalBinder();

    public static PlaybackService getInstance(){
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.logToFile("create service");
        queueManager = QueueManager.getInstance();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        initMediaSession();
        registerNotificationReceiver();
    }

    private void initMediaSession() {
        mediaSession = new MediaSession(this, "MusicPlayerSession");
        mediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public void onPlay() { play(); }

                @Override
                public void onPause() { pause(); }

                @Override
                public void onSkipToNext() { next(); }

                @Override
                public void onSkipToPrevious() { previous(); }

                @Override
                public void onSeekTo(long pos) { seekTo((int) pos); }

                @Override
                public void onStop() { stopAndDestroyService(); }

                @Override
                public void onCustomAction(String action, android.os.Bundle extras) {
                    if (ACTION_TOGGLE_REPEAT.equals(action)) {
                        toggleRepeatMode();
                    } else if (ACTION_TOGGLE_SHUFFLE.equals(action)) {
                        toggleShuffleMode();
                    } else if (ACTION_CLOSE.equals(action)) {
                        stopAndDestroyService();
                    }
                }
            });

        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                              MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent piMain = PendingIntent.getActivity(this, 0, intent, 
                                                         PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

// חובה כדי שהנגן המערכתי ב-Quick Settings ידע לאן לחזור בלחיצה
        mediaSession.setSessionActivity(piMain);
    }

    private void registerNotificationReceiver() {
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    handleAction(action);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREV);
        filter.addAction(ACTION_STOP);
        filter.addAction(ACTION_CLOSE);
        filter.addAction(ACTION_TOGGLE_REPEAT);
        filter.addAction(ACTION_TOGGLE_SHUFFLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(notificationReceiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.logToFile("start service");
        instance = this;
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_INIT_TRACK)) {
                if (intent.getStringExtra("path") != null) {
                    initTrack(intent.getStringExtra("path"));
                }
            } else {
                handleAction(intent.getAction());
            }
        }
        return START_STICKY;
    }

    private void handleAction(String action) {
        switch (action) {
            case ACTION_PLAY_PAUSE: playPause(); break;
            case ACTION_NEXT: next(); break;
            case ACTION_PREV: previous(); break;
            case ACTION_STOP:
            case ACTION_CLOSE: stopAndDestroyService(); break;
            case ACTION_TOGGLE_REPEAT: toggleRepeatMode(); break;
            case ACTION_TOGGLE_SHUFFLE: toggleShuffleMode(); break;
        }
    }

    public void setActionOrder(ActionType[] order, int[] compactIndices) {
        if (order != null) this.notificationActionOrder = order;
        if (compactIndices != null) this.compactViewIndices = compactIndices;
        updateNotificationAndSession();
    }

    private void initTrack(String path) {
        try {
            File file = new File(path);
            if (file.exists() && file.canRead()) {
                isPlaying = false;
                if (mediaPlayer == null) {
                    LogUtil.logToFile("Initializing MediaPlayer...");
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build();
                    mediaPlayer.setAudioAttributes(audioAttributes);

                    mediaPlayer.setOnPreparedListener(this);
                    mediaPlayer.setOnCompletionListener(this);
                    mediaPlayer.setOnErrorListener(this);
                } else {
                    mediaPlayer.reset();
                }

                mediaPlayer.setDataSource(path);
                isPreparing = true;
                mediaPlayer.prepareAsync();
            } else {
                LogUtil.logToFile("File does not exist or cannot be read: " + path);
            }
        } catch (Throwable e) {
            LogUtil.logToFile(e);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        try {
            isPreparing = false;
            requestAudioFocus();
            mp.start();
            isPlaying = true;

            updateMediaSessionMetadata();
            updateMediaSessionState(PlaybackState.STATE_PLAYING);
            startForegroundNotification();
            startProgressUpdater();
        } catch (Throwable e) {
            LogUtil.logToFile(e);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        LogUtil.logToFile("Track completion reached");
        if (stopAfterTrack) {
            pause();
            return;
        }
        int mode = queueManager.getRepeatMode();
        if (mode == QueueManager.REPEAT_ONE) {
            mp.seekTo(0);
            mp.start();
            updateMediaSessionState(PlaybackState.STATE_PLAYING);
        } else {
            String nextPath = queueManager.getNext();
            if (nextPath != null) {
                initTrack(nextPath);
            } else {
                stopAndDestroyService();
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LogUtil.logToFile("MediaPlayer Error: what=" + what + ", extra=" + extra);
        isPreparing = false;
        String next = queueManager.getNext();
        if (next != null) {
            initTrack(next);
        } else {
            stopAndDestroyService();
        }
        return true;
    }

    public void playPause() {
        if (mediaPlayer == null && queueManager.getCurrentTrack() != null) {
            initTrack(queueManager.getCurrentTrack().getPath());
            return;
        }
        if (mediaPlayer == null) return;

        if (isPlaying) {
            pause();
        } else {
            play();
        }
    }

    public void play() {
        if (mediaPlayer != null && !isPlaying) {
            requestAudioFocus();
            mediaPlayer.start();
            isPlaying = true;
            updateMediaSessionState(PlaybackState.STATE_PLAYING);
            startForegroundNotification();
            startProgressUpdater();
        }
    }

    public void pause() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            updateMediaSessionState(PlaybackState.STATE_PAUSED);
            stopProgressUpdater();
            startForegroundNotification();
        }
    }

    public void next() {
        String nextPath = queueManager.getNext();
        if (nextPath != null) initTrack(nextPath);
    }

    public void previous() {
        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 10000) {
            mediaPlayer.seekTo(0);
            updateMediaSessionState(isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED);
        } else {
            String prevPath = queueManager.getPrevious();
            if (prevPath != null) initTrack(prevPath);
        }
    }

    public void seekTo(int msec) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(msec);
            updateMediaSessionState(isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED);
        }
    }

    public void toggleRepeatMode() {
        int mode = queueManager.getRepeatMode();
        mode = (mode + 1) % 3;
        queueManager.setRepeatMode(mode);
        updateNotificationAndSession();
    }

    public void toggleShuffleMode() {
        queueManager.toggleShuffle();
        updateNotificationAndSession();
    }

    private void updateNotificationAndSession() {
        updateMediaSessionState(isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED);
        startForegroundNotification();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && !isPreparing) return mediaPlayer.getCurrentPosition();
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && !isPreparing) return mediaPlayer.getDuration();
        return 0;
    }

    public boolean isPlaying() { return isPlaying; }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(new android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                           .setAudioAttributes(new AudioAttributes.Builder()
                                                               .setUsage(AudioAttributes.USAGE_MEDIA)
                                                               .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                               .build())
                                           .setAcceptsDelayedFocusGain(true)
                                           .setOnAudioFocusChangeListener(this)
                                           .build());
        } else {
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer != null) mediaPlayer.setVolume(0.2f, 0.2f);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    play();
                }
                break;
        }
    }

    private void updateMediaSessionMetadata() {
        Track current = queueManager.getCurrentTrack();
        if (current == null) return;

        Bitmap art = getAlbumArt(current.getPath());
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, current.getTitle())
            .putString(MediaMetadata.METADATA_KEY_ARTIST, current.getArtist())
            .putLong(MediaMetadata.METADATA_KEY_DURATION, getDuration());

        if (art != null) {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art);
        }

        mediaSession.setMetadata(metadataBuilder.build());
    }
    private void updateMediaSessionState(int state) {
        long actions = PlaybackState.ACTION_PLAY_PAUSE |
            PlaybackState.ACTION_PLAY |
            PlaybackState.ACTION_PAUSE |
            PlaybackState.ACTION_SKIP_TO_NEXT |
            PlaybackState.ACTION_SKIP_TO_PREVIOUS |
            PlaybackState.ACTION_SEEK_TO |
            PlaybackState.ACTION_STOP;

        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
            .setActions(actions)
            .setState(state, getCurrentPosition(), 1.0f, SystemClock.elapsedRealtime());

        // חובה לאנדרואיד 13+: כאן מוסיפים את הכפתורים המיוחדים (X, Shuffle, Repeat)
        // המערכת תיקח אותם מכאן ותציג אותם בצידי הנגן המערכתי
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean isShuffle = queueManager.isShuffle();
            int shuffleIcon = isShuffle ? R.drawable.ic_shuffle : R.drawable.ic_shuffle_off;
            stateBuilder.addCustomAction(ACTION_TOGGLE_SHUFFLE, "Shuffle", shuffleIcon);
            int repeatMode = queueManager.getRepeatMode();
            int repeatIcon = (repeatMode == QueueManager.REPEAT_ONE) ? R.drawable.ic_repeat_song :
                (repeatMode == QueueManager.REPEAT_ALL) ? R.drawable.ic_repeat_queue : R.drawable.ic_next_song;
            stateBuilder.addCustomAction(ACTION_TOGGLE_REPEAT, "Repeat", repeatIcon);
            stateBuilder.addCustomAction(ACTION_CLOSE, "Close", R.drawable.ic_close);
        }

        mediaSession.setPlaybackState(stateBuilder.build());
    }
    /*
    private void updateMediaSessionState(int state) {
        long actions = PlaybackState.ACTION_PLAY_PAUSE |
            PlaybackState.ACTION_PLAY |
            PlaybackState.ACTION_PAUSE |
            PlaybackState.ACTION_SKIP_TO_NEXT |
            PlaybackState.ACTION_SKIP_TO_PREVIOUS |
            PlaybackState.ACTION_SEEK_TO |
            PlaybackState.ACTION_STOP;

        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
            .setActions(actions)
            .setState(state, getCurrentPosition(), 1.0f, SystemClock.elapsedRealtime());

        mediaSession.setPlaybackState(stateBuilder.build());
    }*/

    private void startForegroundNotification() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification() {
        createNotificationChannel();

        Track current = queueManager.getCurrentTrack();
        String title = current != null ? current.getTitle() : "Music Player";
        String artist = current != null ? current.getArtist() : "";
        Bitmap art = current != null ? getAlbumArt(current.getPath()) : null;

        int pendingIntentFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent piMain = PendingIntent.getActivity(this, 0,
                                                         new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                                                         pendingIntentFlags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);

        builder.setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_launcher_notification)
            .setContentIntent(piMain)
            .setPriority(Notification.PRIORITY_MAX)
            .setOngoing(isPlaying)
            .setShowWhen(false);

        if (art != null) {
            builder.setLargeIcon(art);
        }

        // בניה דינמית של ה-Actions לפי הסדר שנבחר
        for (ActionType type : notificationActionOrder) {
            Notification.Action action = createNotificationAction(type, pendingIntentFlags);
            if (action != null) {
                builder.addAction(action);
            }
        }

        // הגדרת MediaStyle והגדרת הכפתורים שיוצגו בתצוגה מוקטנת (Compact)
        Notification.MediaStyle mediaStyle = new Notification.MediaStyle()
            .setMediaSession(mediaSession.getSessionToken());

        if (compactViewIndices != null && compactViewIndices.length > 0) {
            mediaStyle.setShowActionsInCompactView(compactViewIndices);
        }

        builder.setStyle(mediaStyle);

        return builder.build();
    }

    private Notification.Action createNotificationAction(ActionType type, int flags) {
        switch (type) {
            case PREV:
                PendingIntent piPrev = PendingIntent.getService(this, 1,
                                                                new Intent(ACTION_PREV).setClass(this, PlaybackService.class), flags);
                return new Notification.Action.Builder(R.drawable.ic_prev, "Previous", piPrev).build();

            case PLAY_PAUSE:
                PendingIntent piPlayPause = PendingIntent.getService(this, 2,
                                                                     new Intent(ACTION_PLAY_PAUSE).setClass(this, PlaybackService.class), flags);
                int icon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
                String label = isPlaying ? "Pause" : "Play";
                return new Notification.Action.Builder(icon, label, piPlayPause).build();

            case NEXT:
                PendingIntent piNext = PendingIntent.getService(this, 3,
                                                                new Intent(ACTION_NEXT).setClass(this, PlaybackService.class), flags);
                return new Notification.Action.Builder(R.drawable.ic_next, "Next", piNext).build();

            case REPEAT:
                PendingIntent piRepeat = PendingIntent.getService(this, 4,
                                                                  new Intent(ACTION_TOGGLE_REPEAT).setClass(this, PlaybackService.class), flags);
                int repeatMode = queueManager.getRepeatMode();
                int repeatIcon = (repeatMode == QueueManager.REPEAT_ONE) ? R.drawable.ic_repeat_song :
                    (repeatMode == QueueManager.REPEAT_ALL) ? R.drawable.ic_repeat_queue : R.drawable.ic_next_song;
                return new Notification.Action.Builder(repeatIcon, "Repeat", piRepeat).build();

            case SHUFFLE:
                PendingIntent piShuffle = PendingIntent.getService(this, 5,
                                                                   new Intent(ACTION_TOGGLE_SHUFFLE).setClass(this, PlaybackService.class), flags);
                boolean isShuffle = queueManager.isShuffle();
                int shuffleIcon = isShuffle ? R.drawable.ic_shuffle : R.drawable.ic_shuffle_off;
                return new Notification.Action.Builder(shuffleIcon, "Shuffle", piShuffle).build();

            case CLOSE:
            case STOP:
                // לחצן X שמבצע סגירה ועצירה מוחלטת
                PendingIntent piClose = PendingIntent.getService(this, 6,
                                                                 new Intent(ACTION_CLOSE).setClass(this, PlaybackService.class), flags);
                return new Notification.Action.Builder(R.drawable.ic_close, "Close", piClose).build();

            default:
                return null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback Controls",
                    NotificationManager.IMPORTANCE_LOW
                );
                chan.setDescription("Media playback controls and progress");
                chan.setShowBadge(false);
                nm.createNotificationChannel(chan);
            }
        }
    }

    private Bitmap getAlbumArt(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) return BitmapFactory.decodeByteArray(art, 0, art.length);
        } catch (Exception ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void startProgressUpdater() {
        stopProgressUpdater();
        sendBroadcast(new Intent("com.example.musicplayer.UIM_UPDATE"));
        sendBroadcast(new Intent("com.example.musicplayer.UI_UPDATE"));

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    Intent intent = new Intent("com.example.musicplayer.PROGRESS_UPDATE");
                    intent.putExtra("position", mediaPlayer.getCurrentPosition());
                    intent.putExtra("duration", mediaPlayer.getDuration());
                    sendBroadcast(intent);
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(progressRunnable);
    }

    private void stopProgressUpdater() {
        sendBroadcast(new Intent("com.example.musicplayer.UI_UPDATE"));
        if (progressRunnable != null) {
            handler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    /**
     * פונקציה לעצירה וסגירה מוחלטת של הנגן, ההתראה והשירות (כמו onDestroy)
     */
    public void stopAndDestroyService() {
        try {
            LogUtil.logToFile("Stopping and destroying service completely");
            isPlaying = false;
            stopProgressUpdater();

            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }

            if (mediaSession != null) {
                mediaSession.setActive(false);
                mediaSession.release();
                mediaSession = null;
            }

            // הסרת ההתראה מ-Foreground ומחיקתה מדשבורד ההתראות
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIFICATION_ID);
            }

            instance = null;
            stopSelf();
        } catch (Throwable t) {
            LogUtil.logToFile(t);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onDestroy() {
        stopAndDestroyService();
        super.onDestroy();
    }

    // ---- Sleep Timer ----
    private Runnable sleepRunnable;
    private boolean waitForTrackEnd = false;
    private boolean stopAfterTrack = false;

    public void setSleepTimer(long delayMillis, boolean waitEnd) {
        cancelSleepTimer();
        waitForTrackEnd = waitEnd;
        sleepRunnable = new Runnable() {
            @Override
            public void run() {
                if (waitForTrackEnd) {
                    stopAfterTrack = true;
                    if (!isPlaying) pause();
                } else {
                    LogUtil.logToFile("Stopping playback from sleep timer");
                    stopAndDestroyService();
                }
            }
        };
        handler.postDelayed(sleepRunnable, delayMillis);
    }

    public void cancelSleepTimer() {
        if (sleepRunnable != null) {
            handler.removeCallbacks(sleepRunnable);
            sleepRunnable = null;
        }
        stopAfterTrack = false;
    }
}
