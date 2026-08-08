package com.example.musicplayer.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.TextView;
import com.example.musicplayer.LogUtil;

public class RuntimeTextView extends TextView {

    public RuntimeTextView(Context context) {
        super(context);
        applyRuntimeTheme();
    }

    public RuntimeTextView(
        Context context,
        AttributeSet attrs
    ) {
        super(context, attrs);
        applyRuntimeTheme();
    }

    public RuntimeTextView(
        Context context,
        AttributeSet attrs,
        int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        applyRuntimeTheme();
    }

    private void applyRuntimeTheme() {
        ThemePalette palette =
            ThemeManager.getCurrentPalette(
            getContext()
        );

        if (palette == null) {
            return;
        }
        LogUtil.logToFile("rtv="+palette.text);

        setTextColor(palette.text);
        setHintTextColor(palette.text);
        setLinkTextColor(palette.text);
    }
}

