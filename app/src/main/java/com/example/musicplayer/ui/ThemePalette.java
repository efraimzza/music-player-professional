package com.example.musicplayer.ui;

public class ThemePalette {

    public int primary;
    public int primaryDark;
    public int accent;
    public int text;
    public int backItem;
    public int background;

    public ThemePalette(
        int primary,
        int primaryDark,
        int accent,
        int text,
        int backItem,
        int background
    ) {
        this.primary = primary;
        this.primaryDark = primaryDark;
        this.accent = accent;
        this.text = text;
        this.backItem = backItem;
        this.background = background;
    }
}

