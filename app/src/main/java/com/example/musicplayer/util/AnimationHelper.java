package com.example.musicplayer.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.LinearInterpolator;

public class AnimationHelper {

    // Common interpolators
    public static final TimeInterpolator ACCEL_DECEL = new AccelerateDecelerateInterpolator();
    public static final TimeInterpolator ACCEL = new AccelerateInterpolator();
    public static final TimeInterpolator DECEL = new DecelerateInterpolator();
    public static final TimeInterpolator OVERSHOOT = new OvershootInterpolator(1.5f);
    public static final TimeInterpolator LINEAR = new LinearInterpolator();

    // Scale a view with overshoot
    public static void scaleView(final View view, float from, float to, long duration, TimeInterpolator interpolator, final Runnable endAction) {
        view.setScaleX(from);
        view.setScaleY(from);
        view.animate()
            .scaleX(to)
            .scaleY(to)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .withEndAction(endAction)
            .start();
    }

    // Fade in/out
    public static void fadeView(final View view, float from, float to, long duration, TimeInterpolator interpolator, final Runnable endAction) {
        view.setAlpha(from);
        view.animate()
            .alpha(to)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .withEndAction(endAction)
            .start();
    }

    // Slide view vertically
    public static void slideViewY(final View view, float from, float to, long duration, TimeInterpolator interpolator) {
        view.setTranslationY(from);
        view.animate()
            .translationY(to)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start();
    }

    // Parallax-like translation of two views
    public static void parallax(View background, View foreground, float bgScale, float fgScale, long duration, TimeInterpolator interpolator) {
        background.animate()
            .scaleX(bgScale).scaleY(bgScale)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start();
        foreground.animate()
            .scaleX(fgScale).scaleY(fgScale)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start();
    }

    // Rotate a view
    public static void rotateView(View view, float fromDeg, float toDeg, long duration, TimeInterpolator interpolator) {
        view.setRotation(fromDeg);
        view.animate()
            .rotation(toDeg)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start();
    }

    // Crossfade between two views (show one, hide another)
    public static void crossfade(final View fadingOut, final View fadingIn, long duration) {
        fadingOut.animate()
            .alpha(0f)
            .setDuration(duration)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    fadingOut.setVisibility(View.GONE);
                }
            });
        fadingIn.setAlpha(0f);
        fadingIn.setVisibility(View.VISIBLE);
        fadingIn.animate()
            .alpha(1f)
            .setDuration(duration)
            .start();
    }
}
