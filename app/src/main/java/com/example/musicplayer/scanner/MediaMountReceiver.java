package com.example.musicplayer.scanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MediaMountReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean scanOnMedia = prefs.getBoolean("scan_on_media", false);
        if (scanOnMedia) {
            String path = intent.getData().getPath();
            MediaScannerManager scanner = new MediaScannerManager(context);
            scanner.mediaScan(path, null);
        }
    }
}
