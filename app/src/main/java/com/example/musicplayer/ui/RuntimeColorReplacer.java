package com.example.musicplayer.ui;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

public final class RuntimeColorReplacer {

    private RuntimeColorReplacer() {
    }

    

    

    private static int replaceColor(
        int color,

        int oldPrimary,
        int newPrimary,

        int oldPrimaryDark,
        int newPrimaryDark,

        int oldAccent,
        int newAccent,

        int oldBackItem,
        int newBackItem
    ) {
        if (color == oldPrimary) {
            return newPrimary;
        }

        if (color == oldPrimaryDark) {
            return newPrimaryDark;
        }

        if (color == oldAccent) {
            return newAccent;
        }

        if (color == oldBackItem) {
            return newBackItem;
        }

        return color;
    }
}

