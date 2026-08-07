package com.example.musicplayer.ui;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.GradientDrawable;
import android.content.Context;
import android.view.View;

public final class RuntimeBackground {

    private RuntimeBackground() {
    }

    public static void apply(
        Context context,
        View view,
        int normalColor,
        int pressedColor,
        float normalRadiusDp,
        float pressedRadiusDp
    ) {
        GradientDrawable normal =
            new GradientDrawable();

        normal.setShape(
            GradientDrawable.RECTANGLE
        );

        normal.setColor(normalColor);
        normal.setCornerRadius(
            dp(context, normalRadiusDp)
        );

        GradientDrawable pressed =
            new GradientDrawable();

        pressed.setShape(
            GradientDrawable.RECTANGLE
        );

        pressed.setColor(pressedColor);
        pressed.setCornerRadius(
            dp(context, pressedRadiusDp)
        );

        StateListDrawable selector =
            new StateListDrawable();

        selector.addState(
            new int[] {
                android.R.attr.state_pressed
            },
            pressed
        );

        selector.addState(
            new int[] {},
            normal
        );

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            view.setBackground(selector);
        } else {
            view.setBackgroundDrawable(selector);
        }
    }

    public static int getPressedColor(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        int brightness =
            (r * 299 + g * 587 + b * 114) / 1000;

        if (brightness > 128) {
            return Color.rgb(
                (int) (r * 0.82f),
                (int) (g * 0.82f),
                (int) (b * 0.82f)
            );
        }

        return Color.rgb(
            r + (255 - r) / 5,
            g + (255 - g) / 5,
            b + (255 - b) / 5
        );
    }

    private static float dp(
        Context context,
        float value
    ) {
        return value * context.getResources()
            .getDisplayMetrics()
            .density;
    }
}

