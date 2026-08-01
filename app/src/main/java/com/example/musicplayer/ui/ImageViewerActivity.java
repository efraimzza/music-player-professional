package com.example.musicplayer.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.example.musicplayer.R;

public class ImageViewerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        final ImageView image = findViewById(R.id.full_image);
        byte[] artBytes = getIntent().getByteArrayExtra("album_art");
        if (artBytes != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
            image.setImageBitmap(bmp);
        }
        // Start zoom animation from the thumbnail bounds
        int[] startBounds = getIntent().getIntArrayExtra("start_bounds");
        if (startBounds != null) {
            image.setLeft(startBounds[0]);
            image.setTop(startBounds[1]);
            image.setRight(startBounds[2]);
            image.setBottom(startBounds[3]);
            image.setScaleType(ImageView.ScaleType.FIT_XY);
            ObjectAnimator.ofFloat(image, "scaleX", (float)startBounds[2]-startBounds[0] / image.getWidth(), 1f).setDuration(300).start();
            // ... similarly for Y and translation; simpler to use shared element but without compat we'll fake it.
        }
    }
}
