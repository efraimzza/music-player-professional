package com.example.musicplayer.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public final class RuntimeColorPicker {

    public interface OnColorPickedListener {
        void onColorPicked(int color);
    }

    private static final String PREFS = "runtime_theme";
    private static final String COLOR = "accent_color";

    private RuntimeColorPicker() {
    }

    public static void show(
        final Activity activity,
        int defaultColor,
        final OnColorPickedListener listener
    ) {
        final SharedPreferences preferences =
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        final int savedColor =
            preferences.getInt(COLOR, defaultColor);

        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(activity, 20), dp(activity, 16),
                          dp(activity, 20), dp(activity, 8));

        final View preview = new View(activity);
        preview.setBackgroundColor(savedColor);

        LinearLayout.LayoutParams previewParams =
            new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(activity, 70)
        );

        layout.addView(preview, previewParams);

        final TextView hexText = new TextView(activity);
        hexText.setTextColor(Color.DKGRAY);
        hexText.setTextSize(16);
        hexText.setPadding(0, dp(activity, 10), 0, dp(activity, 8));
        layout.addView(hexText);

        final SeekBar redBar = createBar(activity);
        final SeekBar greenBar = createBar(activity);
        final SeekBar blueBar = createBar(activity);

        redBar.setProgress(Color.red(savedColor));
        greenBar.setProgress(Color.green(savedColor));
        blueBar.setProgress(Color.blue(savedColor));

        layout.addView(createLabel(activity, "Red"));
        layout.addView(redBar);

        layout.addView(createLabel(activity, "Green"));
        layout.addView(greenBar);

        layout.addView(createLabel(activity, "Blue"));
        layout.addView(blueBar);

        final SeekBar[] bars = {
            redBar,
            greenBar,
            blueBar
        };

        SeekBar.OnSeekBarChangeListener changeListener =
            new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(
                SeekBar seekBar,
                int progress,
                boolean fromUser
            ) {
                int color = Color.rgb(
                    bars[0].getProgress(),
                    bars[1].getProgress(),
                    bars[2].getProgress()
                );

                preview.setBackgroundColor(color);
                hexText.setText(toHex(color));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        redBar.setOnSeekBarChangeListener(changeListener);
        greenBar.setOnSeekBarChangeListener(changeListener);
        blueBar.setOnSeekBarChangeListener(changeListener);

        hexText.setText(toHex(savedColor));

        new AlertDialog.Builder(activity)
            .setTitle("Choose theme color")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(
                    DialogInterface dialog,
                    int which
                ) {
                    int color = Color.rgb(
                        redBar.getProgress(),
                        greenBar.getProgress(),
                        blueBar.getProgress()
                    );

                    preferences.edit()
                        .putInt(COLOR, color)
                        .apply();

                    if (listener != null) {
                        listener.onColorPicked(color);
                    }
                }
            })
            .show();
    }

    public static int getSavedColor(
        Context context,
        int defaultColor
    ) {
        return context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(COLOR, defaultColor);
    }

    private static SeekBar createBar(Context context) {
        SeekBar bar = new SeekBar(context);
        bar.setMax(255);
        return bar;
    }

    private static TextView createLabel(
        Context context,
        String text
    ) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(14);
        return label;
    }

    private static String toHex(int color) {
        return String.format(
            "#%02X%02X%02X",
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        );
    }

    private static int dp(Context context, int value) {
        return (int) (
            value * context.getResources()
            .getDisplayMetrics().density + 0.5f
            );
    }
}

