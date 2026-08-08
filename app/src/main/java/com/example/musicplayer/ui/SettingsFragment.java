package com.example.musicplayer.ui;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.DocumentsContract;
import android.widget.TimePicker;
import android.widget.Toast;
import com.example.musicplayer.R;
import com.example.musicplayer.service.PlaybackService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import android.os.Handler;
import android.os.Looper;
import com.example.musicplayer.LogUtil;
import com.example.musicplayer.MediaDataManager;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.TimePicker;
import android.widget.Toast;
import com.example.musicplayer.MediaDataManager;
import com.example.musicplayer.R;
import com.example.musicplayer.scanner.MediaScannerManager;
import com.example.musicplayer.service.PlaybackService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class SettingsFragment extends PreferenceFragment {

    private static final int REQUEST_RESTORE = 100;
    private static final int REQUEST_PICK_FOLDER = 200;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        // Backup & restore
        findPreference("backup").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    backupDatabase();
                    return true;
                }
            });
        findPreference("restore").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    pickRestoreFile();
                    return true;
                }
            });

        // Sleep timer
        findPreference("sleep_timer").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showSleepTimerDialog();
                    return true;
                }
            });

        // Clear DB
        findPreference("clear_db").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ((MainActivity) getActivity()).getMgr().clearDatabase();
                    Toast.makeText(getActivity(), "Database cleared", Toast.LENGTH_SHORT).show();
                    //reinit db...
                    ((MainActivity) getActivity()).initMgr();
                    //old
                    //((MainActivity) getActivity()).scanToDatabase();
                    //new
                    scanAll();
                    Toast.makeText(getActivity(), "Database scan...", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

        // Scan all storage
        findPreference("scan_all").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    scanAll();
                    return true;
                }
            });

        // Scan music folders only
        findPreference("scan_music_folders").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    scanMusicFolders();
                    return true;
                }
            });

        // Pick folder to scan (opens our custom folder picker)
        findPreference("pick_folder").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivityForResult(new Intent(getActivity(), FolderPickerActivity.class),
                                           REQUEST_PICK_FOLDER);
                    return true;
                }
            });

        // System media scan (full, using MediaScannerConnection)
        findPreference("system_media_scan").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //systemMediaScan();
                    showThemeChooser();
                    return true;
                }
            });
        findPreference("scan_all").setSummary("שירים="+((MainActivity) getActivity()).getMgr().getCountDb());
        // Toggle preferences – they are checkboxes defined in XML
    }
    public void showThemeChooser() {
        final String[] names = {
            "Purple",
            "Purple Dark",
            "Red",
            "Red Dark",
            "Teal",
            "Teal Dark",
            "Blue",
            "Blue Dark",
            "Amber",
            "Amber Dark",
            "Orange",
            "Orange Dark",
            "Green",
            "Green Dark",
            "Classic",
            "Classic Dark",
            "GRD",
            "GRD Dark",
            "PU",
            "PU Dark",
            "MO",
            "MO Dark",
            "Custom"
        };

        final int[] ids = {
            ThemeManager.PURPLE,
            ThemeManager.PURPLE_DARK,
            ThemeManager.R_THEME,
            ThemeManager.R_THEME_DARK,
            ThemeManager.TEAL,
            ThemeManager.TEAL_DARK,
            ThemeManager.BLUE,
            ThemeManager.BLUE_DARK,
            ThemeManager.AMBER,
            ThemeManager.AMBER_DARK,
            ThemeManager.ORANGE,
            ThemeManager.ORANGE_DARK,
            ThemeManager.GREEN,
            ThemeManager.GREEN_DARK,
            ThemeManager.CLASSIC,
            ThemeManager.CLASSIC_DARK,
            ThemeManager.GRD,
            ThemeManager.GRD_DARK,
            ThemeManager.PU,
            ThemeManager.PU_DARK,
            ThemeManager.MO,
            ThemeManager.MO_DARK,
            ThemeManager.CUSTOM
        };

        new AlertDialog.Builder(getActivity())
            .setTitle("Choose theme")
            .setItems(
            names,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(
                    DialogInterface dialog,
                    int which
                ) {
                    int selectedId = ids[which];

                    if (selectedId == ThemeManager.CUSTOM) {
                        showCustomThemeEditor();
                    } else {
                        ThemeManager.selectTheme(
                            getActivity(),
                            selectedId
                        );

                        getActivity().recreate();
                    }
                }
            }
        )
            .show();
    }
    
    private void showCustomThemeEditor() {
        final ThemePalette oldPalette =
            ThemeManager.getCurrentPalette(getActivity());

        final int[] colors = {
            oldPalette.primary,
            oldPalette.primaryDark,
            oldPalette.accent,
            oldPalette.backItem,
            oldPalette.text,
            oldPalette.background
        };

        chooseNextCustomColor(colors, 0);
    }
    
    private void chooseNextCustomColor(
        final int[] colors,
        final int index
    ) {
        if (index >= colors.length) {
            ThemeManager.saveCustomTheme(
                getActivity(),
                new ThemePalette(
                    colors[0], // primary
                    colors[1], // primaryDark
                    colors[2], // accent
                    colors[3], // backItem
                    colors[4], // text
                    colors[5]  // background
                )
            );

            getActivity().recreate();
            return;
        }
        LogUtil.logToFile("i="+index+"c="+colors[index]);
        String title="";
        switch(index){
            case 0:
                title="ראשוני";
                break;
            case 1:
                title="ראשוני שחור";
                break;
            case 2:
                title="שני";
                break;
            case 3:
                title="רקע לפריט";
                break;
            case 4:
                title="טקסט";
                break;
            case 5:
                title="רקע";
                break;
        }
        RuntimeColorPicker.show(
            getActivity(),
            title,
            colors[index],
            new RuntimeColorPicker.OnColorPickedListener() {
                @Override
                public void onColorPicked(int color) {
                    colors[index] = color;

                    chooseNextCustomColor(
                        colors,
                        index + 1
                    );
                }
            }
        );
    }
    
    private void scanAll() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean skipNoMedia = prefs.getBoolean("skip_nomedia", true);
        MediaScannerManager scanner = ((MainActivity) getActivity()).mscanner;
        scanner.fullScan(skipNoMedia, new MediaScannerManager.ScanCallback() {
                @Override
                public void onScanComplete(int filesAdded) {
                    if(getActivity()!=null)
                    Toast.makeText(getActivity(), "הסריקה המלאה הסתימה. נוספו " + filesAdded + " קבצים.",
                                   Toast.LENGTH_LONG).show();
                }
            });
    }

    private void scanMusicFolders() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean skipNoMedia = prefs.getBoolean("skip_nomedia", true);
        MediaScannerManager scanner = ((MainActivity) getActivity()).mscanner;
        scanner.scanMusicDirectories(skipNoMedia, new MediaScannerManager.ScanCallback() {
                @Override
                public void onScanComplete(int filesAdded) {
                    if(getActivity()!=null)
                    Toast.makeText(getActivity(), "Music folders scan finished. Added " + filesAdded + " files.",
                                   Toast.LENGTH_LONG).show();
                }
            });
    }

    private void systemMediaScan() {
        MediaScannerManager scanner = ((MainActivity) getActivity()).mscanner;
        // Scan external storage root using system scanner
        scanner.mediaScan(Environment.getExternalStorageDirectory().getAbsolutePath(),
            new MediaScannerManager.ScanCallback() {
                @Override
                public void onScanComplete(int filesAdded) {
                    if(getActivity()!=null)
                    Toast.makeText(getActivity(), "System media scan triggered.",
                                   Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESTORE && resultCode == getActivity().RESULT_OK && data != null) {
            // restore code (same as before)
            Uri uri = data.getData();
            try {
                if(getActivity()!=null){
                File db = getActivity().getDatabasePath("musicplayer.db");
                FileInputStream fis = (FileInputStream) getActivity().getContentResolver().openInputStream(uri);
                FileOutputStream fos = new FileOutputStream(db);
                byte[] buf = new byte[4096];
                int len;
                while ((len = fis.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
                fis.close(); fos.close();
                
                Toast.makeText(getActivity(), "Database restored. Please restart.", Toast.LENGTH_LONG).show();
                
                getActivity().recreate();
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Restore failed", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PICK_FOLDER && resultCode == getActivity().RESULT_OK && data != null) {
            String folder = data.getStringExtra("folder_path");
            if (folder != null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                boolean skipNoMedia = prefs.getBoolean("skip_nomedia", true);
                MediaScannerManager scanner = ((MainActivity) getActivity()).mscanner;
                scanner.scanFolder(new File(folder), skipNoMedia, new MediaScannerManager.ScanCallback() {
                        @Override
                        public void onScanComplete(int filesAdded) {
                            if(getActivity()!=null)
                            Toast.makeText(getActivity(), "Folder scan finished. Added " + filesAdded + " files.",
                                           Toast.LENGTH_LONG).show();
                        }
                    });
            }
        }
    }
 

    

    

    private void backupDatabase() {
        try {
            if(getActivity()!=null){
            File db = getActivity().getDatabasePath("musicplayer.db");
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!exportDir.exists()) exportDir.mkdirs();
            File backup = new File(exportDir, "musicplayer_backup.db");
            FileInputStream fis = new FileInputStream(db);
            FileOutputStream fos = new FileOutputStream(backup);
            FileChannel in = fis.getChannel();
            FileChannel out = fos.getChannel();
            in.transferTo(0, in.size(), out);
            fis.close(); fos.close(); in.close(); out.close();
            Toast.makeText(getActivity(), "Backup saved to " + backup.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void pickRestoreFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_RESTORE);
    }

    

    private void showSleepTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Sleep Timer");
        String[] items = {"15 minutes", "30 minutes", "45 minutes", "60 minutes", "Custom"};
        builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: setTimer(15); break;
                        case 1: setTimer(30); break;
                        case 2: setTimer(45); break;
                        case 3: setTimer(60); break;
                        case 4: showCustomTimerDialog(); break;
                    }
                }
            });
        builder.show();
    }

    private void showCustomTimerDialog() {
        TimePickerDialog d = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    setTimer(hourOfDay * 60 + minute);
                }
            }, 0, 0, true);
        d.setTitle("Set duration");
        d.show();
    }

    private void setTimer(final int minutes) {
        new AlertDialog.Builder(getActivity())
            .setTitle("Wait until track ends?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startServiceWithTimer(minutes * 60 * 1000L, true);
                }
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startServiceWithTimer(minutes * 60 * 1000L, false);
                }
            }).show();
    }

    private void startServiceWithTimer(long millis, boolean waitEnd) {
        //PlaybackService svc = ((MainActivity) getActivity()).getService();
        PlaybackService svc=PlaybackService.getInstance();
        if (svc != null) {
            svc.setSleepTimer(millis, waitEnd);
            if(getActivity()!=null)
            Toast.makeText(getActivity(), "Sleep timer set", Toast.LENGTH_SHORT).show();
        }
    }
}
