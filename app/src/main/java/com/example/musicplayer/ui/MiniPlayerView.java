package com.example.musicplayer.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.musicplayer.R;
import android.graphics.Bitmap;

public class MiniPlayerView extends FrameLayout {
    private View miniBar;
    private View fullPlayer;
    private ImageView albumArt;
    private TextView titleText, artistText;
    private boolean isExpanded = false;
    private float dragStartY;
    private static final float EXPANDED_TRANSLATION_Y = 0; // top of screen
    private static final float COLLAPSED_TRANSLATION_Y_OFFSET = 0; // we'll calculate

    public MiniPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.mini_player, this);
        miniBar = findViewById(R.id.mini_bar);
        fullPlayer = findViewById(R.id.full_player);
        albumArt = findViewById(R.id.mini_album_art);
        titleText = findViewById(R.id.mini_title);
        artistText = findViewById(R.id.mini_artist);

        // Collapse initially
        fullPlayer.setTranslationY(getResources().getDisplayMetrics().heightPixels);
        miniBar.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //toggle();
                }
            });
        fullPlayer.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // handle drag down to collapse
                    return onFullPlayerTouch(event);
                }
            });
    }

    private void toggle() {
        if (isExpanded) collapse();
        else expand();
    }

    public void expand() {
        int screenHeight = getHeight();
        ObjectAnimator anim = ObjectAnimator.ofFloat(fullPlayer, "translationY",
                                                     screenHeight, 0);
        anim.setDuration(350);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
        isExpanded = true;
    }

    public void collapse() {
        int screenHeight = getHeight();
        ObjectAnimator anim = ObjectAnimator.ofFloat(fullPlayer, "translationY",
                                                     0, screenHeight);
        anim.setDuration(350);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // fullPlayer hidden
                }
            });
        anim.start();
        isExpanded = false;
    }

    private boolean onFullPlayerTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragStartY = event.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float delta = event.getRawY() - dragStartY;
                if (delta > 0) { // dragging down
                    fullPlayer.setTranslationY(delta);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (fullPlayer.getTranslationY() > getHeight() / 3) {
                    collapse();
                } else {
                    expand(); // snap back
                }
                return true;
        }
        return false;
    }

    // Update mini info
    public void setTrackInfo(String title, String artist,Bitmap art) {
        titleText.setText(title);
        artistText.setText(artist);
        if(art!=null)albumArt.setImageBitmap(art);else albumArt.setImageResource(R.drawable.default_art);
    }
}
