package com.example.musicplayer.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.musicplayer.R;
import com.example.musicplayer.service.PlaybackService;
import android.widget.Toast;
import com.example.musicplayer.MediaDataManager;
import android.os.Environment;
import android.os.AsyncTask;
import java.io.File;
import com.example.musicplayer.LogUtil;
import com.example.musicplayer.QueueManager;
import android.view.ViewGroup;
import android.view.Gravity;
import com.example.musicplayer.util.AnimationHelper;
import com.example.musicplayer.Track;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.graphics.BitmapFactory;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.example.musicplayer.scanner.MediaScannerManager;
import android.Manifest;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.os.Build;
import android.net.Uri;

public class MainActivity extends BaseActivity {
    boolean minit=false;
    private LinearLayout bottomNav;
    //private PlaybackService service;
    private boolean bound = false;
    MediaDataManager mgr;
    MediaScannerManager mscanner;
    public static boolean scanning=false;
    public static String curScanDir="";
    public MiniPlayerView minipl=null;
    private int savedPage = 0;
    private long firstBackTime;
    /*private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((PlaybackService.LocalBinder) binder).getService();
            if(service!=null)
            bound = true;
            else unbindService(this);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            service=null;
        }
    };*/
    private BroadcastReceiver uiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMiniPl();
        }
    };
    private FragmentPager pager;
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pager != null) {
            outState.putInt("SAVED_PAGE", pager.getCurrentPage());
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. ביטול שחזור הפרגמנטים האוטומטי שגורם לכפילויות ובלאגן בסיבוב
        if (savedInstanceState != null) {
            savedPage = savedInstanceState.getInt("SAVED_PAGE", 0);
            savedInstanceState.remove("android:fragments"); // עבור Fragment רגיל
            savedInstanceState.remove("android:support:fragments"); // למקרה שנעשה שימוש ב-AndroidX
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try{
            if(getActionBar().isShowing())
                getActionBar().hide();
        }catch(Exception e){}
        mcheckPerm();
    }
    void mcheckPerm(){
        try{
            requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"},55);
        }catch(Exception e){}
        if(!hasManageExternalStoragePermission(this)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestManageExternalStoragePermission(this);
            } else if (!hasWriteExternalStoragePermission(this)) {
                requestWriteExternalStoragePermission(this);
            }else{
                inital();
            }
        }else{
            inital();
        }
    }
    void inital(){
        //LogUtil.logToFile("in="+minit);
        if(minit)return;
        minit=true;
        try{
            //checkAndRequestPermissions();
            initMgr();
            if(!getDatabasePath("musicplayer.db").exists()||mgr.getCountDb()==0){
                //old
                //scanToDatabase();
                //new
                scanAll();
            }
            bottomNav = findViewById(R.id.bottom_nav);
            pager = findViewById(R.id.pager);
            pager.setFragmentManager(getFragmentManager());

            List<Fragment> fragList = new ArrayList<>();
            fragList.add(new PlayerFragment());
            fragList.add(new FoldersFragment());
            fragList.add(new AlbumsFragment());
            fragList.add(new ArtistFragment());
            fragList.add(new GenreFragment());
            fragList.add(new QueueFragment());
            fragList.add(new SearchFragment());
            fragList.add(new SettingsFragment());
            pager.setFragments(fragList);
            
            
            setupTab(0, "Player");
            setupTab(1, "Folders");
            setupTab(2, "Albums");
            setupTab(3, "Artist");
            setupTab(4, "Genre");
            setupTab(5, "Queue");
            setupTab(6, "Search");
            setupTab(7, "Settings");
            // 2. מעבר לעמוד שהיינו בו לפני סיבוב המסך
            if (savedPage != 0) {
                pager.post(new Runnable() {
                        @Override
                        public void run() {
                            pager.goToPage(savedPage, false); // false = קפיצה מיידית ללא אנימציה
                        }
                    });
            }
            /*
             // Connect tabs to pager
             tabPlayer.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) { pager.goToPage(0, true); }
             });
             */
/*
            setupTab( new PlayerFragment(), "Player");
            setupTab(new FoldersFragment(), "Folders");
            setupTab(new AlbumsFragment(), "Albums");
            setupTab( new ArtistFragment(), "Artist");
            setupTab(new GenreFragment(), "Genre");
            setupTab(new QueueFragment(), "Queue");
            setupTab( new SearchFragment(), "Search");
            setupTab(new SettingsFragment(), "Settings");
*/
            //switchFragment(new PlayerFragment(), "Player", false);

            //bindService(new Intent(this, PlaybackService.class), connection, BIND_AUTO_CREATE);
            
            minipl= findViewById(R.id.mini_player);
        }catch(Throwable e){LogUtil.logToFile(e);}
    }
    Fragment curFragment=null;
    /*private void setupTab( final Fragment fragment, final String tag) {
        //TextView tab = findViewById(resId);
        TextView tab = new TextView(this);
        tab.setText(tag);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT);
        lp.weight=1.0f;
        tab.setLayoutParams(lp);
        tab.setGravity(Gravity.CENTER);
        tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //switchFragment(fragment, tag, true);
                    if(curFragment!=null)
                    if(!curFragment.equals(fragment))
                    switchFragmentWithParallax(curFragment,fragment,tag);
                    curFragment=fragment;
                }
            });
        //bottomNav.addView(tab);
    }*/
    private void setupTab(final int pageIndex, final String tag) {
        TextView tab = new RuntimeTextView(this);
        String name="";
        switch (tag){
            case "Player":
                name="נגן";
                break;
            case "Folders":
                name="תיקיות";
                break;
            case "Albums":
                name="אלבומים";
                break;
            case "Artist":
                name="אמנים";
                break;
            case "Genre":
                name="סגנונות";
                break;
            case "Queue":
                name="תור";
                break;
            case "Search":
                name="חיפוש";
                break;
            case "Settings":
                name="הגדרות";
                break;
        }
        tab.setText(name);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.weight = 1.0f;
        tab.setLayoutParams(lp);
        tab.setGravity(Gravity.CENTER);

        // אופציונלי: להוסיף אפקט לחיצה מובנה של אנדרואיד
        // tab.setBackgroundResource(android.R.attr.selectableItemBackground);

        tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (pager != null) {
                        // מעביר את ה-FragmentPager לעמוד המתאים לפי האינדקס
                        pager.goToPage(pageIndex, true); 
                    }
                }
            });

        // חשוב: להוסיף את הטאב לשורת הניווט התחתונה!
        bottomNav.addView(tab);
    }
// Inside MainActivity:
    public void switchFragmentWithParallax(Fragment from, Fragment to, String tag) {
        /*FragmentTransaction ft = getFragmentManager().beginTransaction();
        // Disable default animations
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        // But we'll run our own on the root views
        final View fromView = from.getView();
        final View toView = to.getView(); // will be created after commit
        ft.replace(R.id.container, to, tag);
        ft.addToBackStack(tag);
        ft.commit();

        if (fromView != null) {
            // Shrink and fade out the old fragment
            AnimationHelper.scaleView(fromView, 1.0f, 0.95f, 250, AnimationHelper.DECEL, null);
            AnimationHelper.fadeView(fromView, 1f, 0f, 250, AnimationHelper.ACCEL, null);
        }
        // Animate the new fragment after it's attached
        getFragmentManager().executePendingTransactions();
        if (toView != null) {
            toView.setScaleX(1.1f);
            toView.setScaleY(1.1f);
            toView.setAlpha(0f);
            AnimationHelper.scaleView(toView, 1.1f, 1.0f, 300, AnimationHelper.OVERSHOOT, null);
            AnimationHelper.fadeView(toView, 0f, 1f, 300, AnimationHelper.DECEL, null);
        }*/
    }
    public void switchFragment(Fragment fragment, String tag, boolean addToBackStack) {
        try{
        /*FragmentTransaction ft = getFragmentManager().beginTransaction();
      //  ft.setCustomAnimations(R.animator.scale_in, R.animator.fade_out,
      //                         R.animator.fade_in, R.animator.scale_out);
        ft.replace(R.id.container, fragment, tag);
        //if (addToBackStack) ft.addToBackStack(null);
        ft.commit();
        curFragment=fragment;*/
        pager.childFrag(fragment,tag);
        }catch(Throwable e){Toast.makeText(this,e.getMessage(),1).show();}
    }

    public PlaybackService getService() {
        //if (!bound) bindService(new Intent(this, PlaybackService.class), connection, BIND_AUTO_CREATE);
        return PlaybackService.getInstance();
    }
    public MediaDataManager getMgr(){return mgr;}
    public void initMgr(){
        mgr = new MediaDataManager(getApplicationContext());
        mscanner = new MediaScannerManager((MainActivity)this);
    }
    private void scanAll() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        boolean skipNoMedia = prefs.getBoolean("skip_nomedia", true);
        MediaScannerManager scanner = mscanner;
        scanner.fullScan(skipNoMedia, new MediaScannerManager.ScanCallback() {
                @Override
                public void onScanComplete(int filesAdded) {
                        Toast.makeText(MainActivity.this, "Full scan finished. Added " + filesAdded + " files.",
                                       Toast.LENGTH_LONG).show();
                }
            });
    }
    void updateMiniPl(){
        //if (!bound) bindService(new Intent(this, PlaybackService.class), connection, BIND_AUTO_CREATE);
        if (getService() == null) return;
        Track track = QueueManager.getInstance().getCurrentTrack();
        if (track != null) {
            Bitmap art = getAlbumArt(track.getPath());
            minipl.setTrackInfo(track.getTitle(),track.getArtist(),art);
        }
    }
    private Bitmap getAlbumArt(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) return BitmapFactory.decodeByteArray(art, 0, art.length);
        } catch (Exception e) {} finally {
            try{
                retriever.release();
            }catch(Exception e){}
        }
        return null;
    }
    /*
    @Override
    protected void onDestroy() {
        unregisterReceiver(uiReceiver);
        //if (bound) unbindService(connection);
        //QueueManager.destroy();
        if(getService()!=null)
        getService().stopForeground(true);
        if(getService()!=null)
        getService().stopSelf();
        LogUtil.logToFile("destroy main");
        super.onDestroy();
    }*/
    @Override
    protected void onDestroy() {
        // 1. חשוב לבטל את הרישום של ה-Receiver בכל מקרה כדי למנוע דליפות זיכרון (Memory Leaks) במסך הישן
        try {
            unregisterReceiver(uiReceiver);
        } catch (Exception e) {}

        // 2. מכבים את השירות *רק* אם ה-Activity נסגר לחלוטין (המשתמש יצא מהאפליקציה), ולא בגלל סיבוב מסך
        if (!isChangingConfigurations()) {
            if(getService()!=null)
                getService().stopForeground(true);
            if(getService()!=null)
                getService().stopSelf();
            LogUtil.logToFile("destroy main - App completely closed, stopping service");
        } else {
            LogUtil.logToFile("destroy main - Screen rotated, letting service run in background");
        }

        super.onDestroy();
    }
    @Override
    protected void onStart() {
        //if (!bound) bindService(new Intent(this, PlaybackService.class), connection, BIND_AUTO_CREATE);
        super.onStart();
        
    }
    @Override
    protected void onResume() {
        if(!minit){
            mcheckPerm();
        }
        //if (!bound) bindService(new Intent(this, PlaybackService.class), connection, BIND_AUTO_CREATE);
        registerReceiver(uiReceiver,
                                       new IntentFilter("com.example.musicplayer.UIM_UPDATE"));
        updateMiniPl();
        super.onResume();
        
    }
    @Override
    protected void onStop() {
        //if (bound) unbindService(connection);
        super.onStop();
    }
    
    public void scanToDatabase() {
        try{
            new Thread(){public void run(){
                    try{
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try{
                    LogUtil.logToFile("start scanning");
                scanning=true;
                scanDir(Environment.getExternalStorageDirectory(), mgr);
                }catch(Throwable e){LogUtil.logToFile(e);}
                scanning=false;
                LogUtil.logToFile("end scanning");
                //update for all...
                return null;
            }
            void scanDir(File dir, MediaDataManager mgr) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isDirectory()) {scanDir(f, mgr);curScanDir=f.getAbsolutePath();}
                        else if (isAudioOrPdf(f) && !f.getName().endsWith(".pdf")) {
                            // extract metadata and insert
                            // Use MediaMetadataRetriever
                            mgr.insertTrack(mgr.extractMetadata(f.getAbsolutePath()));
                        }
                    }
                }
            }
        }.execute();
                    }catch(Throwable e){LogUtil.logToFile(e);}
        }}.start();
    }catch(Throwable e){LogUtil.logToFile(e);}
    }
    private boolean isAudioOrPdf(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".flac") ||
            name.endsWith(".ogg") || name.endsWith(".wav") ||
            name.endsWith(".m4a") || name.endsWith(".pdf");
    }

    @Override
    public void onBackPressed() {
        //if can go back in folders goBack
        //if isnt in player goto player
        if(getService()!=null)
            LogUtil.logToFile("sip="+getService().isPlaying());
        if(!pager.gotoPlayer()){
            if(getService()!=null)if(getService().isPlaying()) moveTaskToBack(true);else {
                    if (System.currentTimeMillis() - firstBackTime > 2000) {
                        Toast.makeText(this, "press again to exit", Toast.LENGTH_SHORT).show();
                        firstBackTime = System.currentTimeMillis();
                        return; 
                    }
                    finish();
                }
        }
        //switchFragment(new PlayerFragment(), "Player", false);
        //moveTaskToBack(true);
    }

    public void endScanning(){
        
    }
    private static final int PERMISSION_REQUEST_CODE = 1001;

    public boolean checkAndRequestPermissions() {
        // בדיקה שאנו רצים על גרסת אנדרואיד התומכת בהרשאות זמן ריצה (API 23 ומעלה)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            // התאמה למערכות הפעלה ישנות (עד 12) וחדשות (13 ומעלה)
            //String permission = android.Manifest.permission.READ_EXTERNAL_STORAGE;
            String permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
            //if (android.os.Build.VERSION.SDK_INT >= 33) { // Android 13+ (API 33)
                //permission = android.Manifest.permission.READ_MEDIA_AUDIO;
            //}

            // בדיקה האם ההרשאה כבר ניתנה בעבר
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // הבקשה עצמה (מציג את חלונית המערכת הקופצת)
                requestPermissions(new String[]{permission}, PERMISSION_REQUEST_CODE);
            } else {
                //android.widget.Toast.makeText(this, "ההרשאה כבר קיימת. הכל תקין.", android.widget.Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        }
        return true;
    }

    // קבלת התשובה מהמשתמש לאחר לחיצה בחלונית האישור
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.widget.Toast.makeText(this, "הרשאת גישה לאחסון התקבלה!", android.widget.Toast.LENGTH_SHORT).show();
                // כאן ניתן לקרוא לטעינה מחודשת של מנהל הקבצים או מאגר המוזיקה
            } else {
                LogUtil.logToFile("read permission granted");
                android.widget.Toast.makeText(this, "ההרשאה נדחתה. לא ניתן לקרוא שירים.", android.widget.Toast.LENGTH_LONG).show();
            }
        }
    }
    // Check if Manage External Storage permission is granted (for Android 11+)
    public static boolean hasManageExternalStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            // For below Android 11, use normal READ/WRITE permissions
            int writePermission = context.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, android.os.Process.myPid(), android.os.Process.myUid());
            return writePermission == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static void requestManageExternalStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                context.startActivity(intent);
            }
        }
    }
    public static boolean hasWriteExternalStoragePermission(Context context) {
        int permissionCheck = context.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, android.os.Process.myPid(), android.os.Process.myUid());
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }
    public static void requestWriteExternalStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
    }
    
}
