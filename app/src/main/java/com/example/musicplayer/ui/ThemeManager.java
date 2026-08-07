package com.example.musicplayer.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;

import com.example.musicplayer.R;

public final class ThemeManager {

    public static final int PURPLE = 1;
    public static final int PURPLE_DARK = 2;

    public static final int R_THEME = 3;
    public static final int R_THEME_DARK = 4;

    public static final int TEAL = 5;
    public static final int TEAL_DARK = 6;

    public static final int BLUE = 7;
    public static final int BLUE_DARK = 8;

    public static final int AMBER = 9;
    public static final int AMBER_DARK = 10;

    public static final int ORANGE = 11;
    public static final int ORANGE_DARK = 12;

    public static final int GREEN = 13;
    public static final int GREEN_DARK = 14;

    public static final int CLASSIC = 15;
    public static final int CLASSIC_DARK = 16;

    public static final int GRD = 17;
    public static final int GRD_DARK = 18;

    public static final int PU = 19;
    public static final int PU_DARK = 20;

    public static final int CUSTOM = 1000;

    private static final String PREFS = "theme_settings";
    private static final String KEY_THEME = "theme_id";

    private ThemeManager() {
    }

    public static void selectTheme(Context context, int themeId) {
        getPrefs(context)
            .edit()
            .putInt(KEY_THEME, themeId)
            .apply();
    }

    public static int getSelectedTheme(Context context) {
        return getPrefs(context).getInt(KEY_THEME, PURPLE);
    }

    /**
     * מחזיר את ה-style XML שייטען ב-Activity.
     */
    public static int getStyle(Context context) {
        switch (getSelectedTheme(context)) {
            case PURPLE:
                return R.style.AppThemePurple;

            case PURPLE_DARK:
                return R.style.AppThemePurpleDark;

            case R_THEME:
                return R.style.AppThemePurpleR;

            case R_THEME_DARK:
                return R.style.AppThemePurpleRDark;

            case TEAL:
                return R.style.AppThemeTeal;

            case TEAL_DARK:
                return R.style.AppThemeTealDark;

            case BLUE:
                return R.style.AppThemeBlue;

            case BLUE_DARK:
                return R.style.AppThemeBlueDark;

            case AMBER:
                return R.style.AppThemeAmber;

            case AMBER_DARK:
                return R.style.AppThemeAmberDark;

            case ORANGE:
                return R.style.AppThemeOrange;

            case ORANGE_DARK:
                return R.style.AppThemeOrangeDark;

            case GREEN:
                return R.style.AppThemeGreen;

            case GREEN_DARK:
                return R.style.AppThemeGreenDark;

            case CLASSIC:
                return R.style.AppThemeClassic;

            case CLASSIC_DARK:
                return R.style.AppThemeClassicDark;

            case GRD:
                return R.style.AppThemeGrd;

            case GRD_DARK:
                return R.style.AppThemeGrdDark;

            case PU:
                return R.style.AppThemePu;

            case PU_DARK:
                return R.style.AppThemePuDark;

            case CUSTOM:
                // Style בסיסי בלבד.
                // הצבעים המותאמים יוחלו לאחר setContentView.
                return R.style.AppThemePurple;

            default:
                return R.style.AppThemePurple;
        }
    }

    public static void saveCustomTheme(
        Context context,
        ThemePalette palette
    ) {
        getPrefs(context)
            .edit()
            .putInt(KEY_THEME, CUSTOM)
            .putInt("primary", palette.primary)
            .putInt("primary_dark", palette.primaryDark)
            .putInt("accent", palette.accent)
            .putInt("back_item", palette.backItem)
            .putInt("text", palette.text)
            .putInt("background", palette.background)
            .apply();
    }

    public static ThemePalette getCurrentPalette(Context context) {
        if (getSelectedTheme(context) == CUSTOM) {
            SharedPreferences p = getPrefs(context);

            return new ThemePalette(
                p.getInt("primary", Color.DKGRAY),
                p.getInt("primary_dark", Color.BLACK),
                p.getInt("accent", Color.BLUE),
                p.getInt("back_item", Color.DKGRAY),
                p.getInt("text", Color.WHITE),
                p.getInt("background", Color.BLACK)
            );
        }

        /*
         * עבור Themes שהוגדרו ב-XML,
         * הצבעים נלקחים ישירות מה-theme הפעיל.
         */
        return new ThemePalette(
            resolveColor(context,
                         android.R.attr.colorPrimary,
                         Color.GRAY),

            resolveColor(context,
                         android.R.attr.colorPrimaryDark,
                         Color.DKGRAY),

            resolveColor(context,
                         android.R.attr.colorAccent,
                         Color.BLUE),

            resolveColor(context,
                         R.attr.colorBackItem,
                         Color.DKGRAY),

            resolveColor(context,
                         R.attr.colorText,
                         Color.WHITE),

            resolveColor(context,
                         android.R.attr.colorBackground,
                         Color.BLACK)
        );
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        );
    }

    private static int resolveColor(
        Context context,
        int attribute,
        int defaultColor
    ) {
        TypedValue value = new TypedValue();

        boolean found = context.getTheme().resolveAttribute(
            attribute,
            value,
            true
        );

        if (!found) {
            return defaultColor;
        }

        if (value.resourceId != 0) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    return context.getResources().getColor(
                        value.resourceId,
                        context.getTheme()
                    );
                } else {
                    return context.getResources().getColor(
                        value.resourceId
                    );
                }
            } catch (Throwable ignored) {
            }
        }

        return value.data;
    }
}

