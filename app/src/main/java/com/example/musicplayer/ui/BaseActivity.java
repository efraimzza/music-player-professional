package com.example.musicplayer.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import com.example.musicplayer.R;

public abstract class BaseActivity extends Activity {

    protected RuntimeInflater.Hook runtimeHook;
    protected ThemePalette palette;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
         * BaseTheme XML מספק מידות, חלונות,
         * transitions וכו'.
         */
         if(ThemeManager.getSelectedTheme(this)!=ThemeManager.CUSTOM){
             setTheme(ThemeManager.getStyle(this));
         }else{
        setTheme(R.style.AppThemeMoDark);
        
        palette = ThemeManager.getCurrentPalette(this);

        /*
         * חייב להיות לפני setContentView.
         */
        runtimeHook = RuntimeInflater.install(
            getLayoutInflater(),
            palette
        );
}
        super.onCreate(savedInstanceState);
    }

    protected void updateRuntimePalette(
        ThemePalette newPalette
    ) {
        palette = newPalette;

        if (runtimeHook != null) {
            runtimeHook.setPalette(newPalette);
        }

        /*
         * Views שכבר קיימים צריכים עדכון.
         * Views חדשים יקבלו את הצבע דרך ה-Hook.
         */
         /*
        RuntimeColorReplacer.applyTextColor(
            findViewById(android.R.id.content),
            newPalette.text
        );
*/
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(
                newPalette.primaryDark
            );

            getWindow().setNavigationBarColor(
                newPalette.background
            );
        }

        if (getActionBar() != null) {
            getActionBar().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(
                    newPalette.primary
                )
            );
        }
    }
}

