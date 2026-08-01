package com.example.musicplayer.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

public class VisualizerView extends View {
    private Paint paint;
    private Random random = new Random();
    private float[] barValues = new float[20];
    private ValueAnimator animator;

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(0xFF00FF00);
        paint.setStrokeWidth(4f);
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    for (int i = 0; i < barValues.length; i++) {
                        barValues[i] = random.nextFloat() * getHeight();
                    }
                    invalidate();
                }
            });
    }

    public void startAnimation() { animator.start(); }
    public void stopAnimation() { animator.cancel(); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float barWidth = getWidth() / (float) barValues.length;
        for (int i = 0; i < barValues.length; i++) {
            float left = i * barWidth;
            float top = getHeight() - barValues[i];
            float right = left + barWidth - 2;
            float bottom = getHeight();
            canvas.drawRect(left, top, right, bottom, paint);
        }
    }
}
