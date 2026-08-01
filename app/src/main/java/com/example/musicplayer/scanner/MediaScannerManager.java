package com.example.musicplayer.scanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.example.musicplayer.MediaDataManager;
import com.example.musicplayer.Track;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.example.musicplayer.LogUtil;
import android.os.Build;
import android.os.storage.StorageVolume;
import android.os.storage.StorageManager;
import com.example.musicplayer.ui.MainActivity;

public class MediaScannerManager {

    private Context context;
    private MediaDataManager dataManager;

    public MediaScannerManager(MainActivity mactivity) {
        this.context = mactivity.getApplicationContext();
        //dataManager = new MediaDataManager(context);
        dataManager = mactivity.getMgr();
    }
    public MediaScannerManager(Context context) {
        this.context = context.getApplicationContext();
        dataManager = new MediaDataManager(context);
    }
    /**
     * Scans a single folder (recursive) and adds audio files to the database.
     * @param rootFolder the folder to scan
     * @param skipNoMedia if true, skip folders containing a .nomedia file
     * @param callback optional callback when done
     */
    public void scanFolder(final File rootFolder, final boolean skipNoMedia,
                           final ScanCallback callback) {
        new AsyncTask<Void, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return scanDir(rootFolder, skipNoMedia);
            }

            @Override
            protected void onPostExecute(Integer count) {
                if (callback != null) callback.onScanComplete(count);
            }
        }.execute();
    }

    private int scanDir(File dir, boolean skipNoMedia) {
        if (skipNoMedia && new File(dir, ".nomedia").exists()) {
            return 0;
        }
        File[] files = dir.listFiles();
        if (files == null) return 0;
        int count = 0;
        for (File f : files) {
            if (f.isDirectory()) {
                MainActivity.curScanDir=f.getAbsolutePath();
                count += scanDir(f, skipNoMedia);
            } else if (isAudioFile(f)) {
                Track track = MediaDataManager.extractMetadata(f.getAbsolutePath());
                dataManager.insertTrack(track);
                count++;
            }
        }
        return count;
    }

    /**
     * Uses Android's MediaScanner to scan a file or directory.
     * This also updates the system media database.
     */
    public void mediaScan(final String path, final ScanCallback callback) {
        MediaScannerConnection.scanFile(context, new String[]{path}, null,
            new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String p, android.net.Uri uri) {
                    MainActivity.scanning=true;
                    // After system scan, also update our DB
                    File file = new File(p);
                    if (file.isFile() && isAudioFile(file)) {
                        Track track = MediaDataManager.extractMetadata(p);
                        dataManager.insertTrack(track);
                    } else if (file.isDirectory()) {
                        scanFolder(file, false, null);
                    }
                    MainActivity.scanning=false;
                    if (callback != null) callback.onScanComplete(1);
                }
            });
    }

    /**
     * Full scan of all available storage volumes (internal, SD card, USB OTG).
     */
    public void fullScan(final boolean skipNoMedia, final ScanCallback callback) {
        new AsyncTask<Void, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                MainActivity.scanning=true;
                int count = 0;
                // Scan standard external storage
                File ext = Environment.getExternalStorageDirectory();
                count += scanDir(ext, skipNoMedia);
                // Scan secondary storage (SD cards, USB)
                File storage = new File("/storage");
                LogUtil.logToFile("lol...");
                if (storage.exists()) {
                    if(Build.VERSION.SDK_INT>=24){
                        StorageManager sm=(StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
                        for (StorageVolume stv : sm.getStorageVolumes()) {
                            if (!stv.isEmulated()) {
                                File vol=new File(storage,stv.getUuid());
                                if (!vol.getAbsolutePath().equals(ext.getAbsolutePath()) &&
                                    vol.isDirectory() && vol.canRead()) {
                                    LogUtil.logToFile(vol.getAbsolutePath());
                                    count += scanDir(vol, skipNoMedia);
                                }
                            }
                            //tv.append(stv.getUuid()+" "+stv.getState()+" "+stv.isEmulated()+" "+stv.isPrimary()+" "+stv.isRemovable());
                        }
                    }else{
                    LogUtil.logToFile("storage.listFiles..."+storage.listFiles());
                    File[] vols = storage.listFiles();
                    if (vols != null) {
                        LogUtil.logToFile("l="+storage.listFiles().length);
                        for (File vol : vols) {
                            // Skip primary external storage (already scanned)
                            if (!vol.getAbsolutePath().equals(ext.getAbsolutePath()) &&
                                vol.isDirectory() && vol.canRead()) {
                                    LogUtil.logToFile(vol.getAbsolutePath());
                                count += scanDir(vol, skipNoMedia);
                            }
                        }
                    }
                    }
                    
                }
                // Also scan /mnt/media_rw for USB OTG on older devices
                File mnt = new File("/mnt/media_rw");
                if (mnt.exists()) {
                    File[] usbs = mnt.listFiles();
                    if (usbs != null) {
                        for (File u : usbs) {
                            if (u.isDirectory() && u.canRead()) {
                                count += scanDir(u, skipNoMedia);
                            }
                        }
                    }
                }
                return count;
            }

            @Override
            protected void onPostExecute(Integer count) {
                MainActivity.scanning=false;
                if (callback != null) callback.onScanComplete(count);
            }
        }.execute();
    }

    /**
     * Scan only standard Music directories.
     */
    public void scanMusicDirectories(final boolean skipNoMedia, final ScanCallback callback) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                MainActivity.scanning=true;
                int count = 0;
                File music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                count += scanDir(music, skipNoMedia);
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                count += scanDir(downloads, skipNoMedia);
                // Additional standard locations
                File audio = new File(Environment.getExternalStorageDirectory(), "Audio");
                count += scanDir(audio, skipNoMedia);
                return count;
            }

            @Override
            protected void onPostExecute(Integer count) {
                MainActivity.scanning=false;
                if (callback != null) callback.onScanComplete(count);
            }
        }.execute();
    }

    private boolean isAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".flac") ||
            name.endsWith(".ogg") || name.endsWith(".wav") ||
            name.endsWith(".m4a") || name.endsWith(".aac") ||
            name.endsWith(".wma");
    }

    public interface ScanCallback {
        void onScanComplete(int filesAdded);
    }
}
