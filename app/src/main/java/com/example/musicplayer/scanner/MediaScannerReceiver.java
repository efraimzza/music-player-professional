package com.example.musicplayer.scanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MediaScannerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean scanOnBoot = prefs.getBoolean("scan_on_boot", false);
        boolean scanOnMedia = prefs.getBoolean("scan_on_media", false); // used in MountReceiver

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && scanOnBoot) {
            MediaScannerManager scanner = new MediaScannerManager(context);
            scanner.fullScan(false, null);
        }
    }
}
