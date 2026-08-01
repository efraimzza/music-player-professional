package com.example.musicplayer.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;
import com.example.musicplayer.LogUtil;
import com.example.musicplayer.R;

public class FragmentPager extends FrameLayout {

    private static final int SNAP_VELOCITY = 600;

    private Scroller scroller;
    private VelocityTracker velocityTracker;
    private float lastTouchX;
    private float lastTouchY;
    private int currentPage = 0;
    private List<Fragment> fragments = new ArrayList<Fragment>();
    private FragmentManager fragmentManager;
    private int activeWidth;

    private String childfragtag = "";

    public FragmentPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        scroller = new Scroller(context, new DecelerateInterpolator(1.5f));
        setWillNotDraw(false);
    }

    public void setFragmentManager(FragmentManager fm) {
        this.fragmentManager = fm;
    }

    public void setFragments(List<Fragment> list) {
        fragments.clear();
        fragments.addAll(list);
        removeAllViews();
        currentPage = 0;
        if (!fragments.isEmpty()) {
            ensureFragmentView(0);
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        activeWidth = getMeasuredWidth();
    }

    public void goToPage(int page, boolean smooth) {
        if (page < 0 || page >= fragments.size()) return;

        if (!childfragtag.equals("")) {
            removeChildFrag();
        }

        if (page != currentPage) {
            if (!already(page)) {
                ensureFragmentView(page);
            }
        }

        currentPage = page;

        if (smooth) {
            int targetX = page * activeWidth;
            int dx = targetX - getScrollX();
            scroller.startScroll(getScrollX(), 0, dx, 0, 400); 
            invalidate();
        } else {
            scrollTo(page * activeWidth, 0);
        }

        showPage(page);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = ev.getX();
                lastTouchY = ev.getY();
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = Math.abs(ev.getY() - lastTouchY);
                float dx = Math.abs(ev.getX() - lastTouchX);
                if (dx > 10 && dx > dy) {
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished()) scroller.abortAnimation();
                lastTouchX = event.getX();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastTouchX;
                int newScrollX = getScrollX() - (int) dx;

                int maxScroll = (fragments.size() - 1) * activeWidth;
                if (newScrollX < 0) newScrollX = 0;
                if (newScrollX > maxScroll) newScrollX = maxScroll;

                scrollTo(newScrollX, 0);
                lastTouchX = event.getX();

                int pageUnder = (newScrollX + (activeWidth / 2)) / activeWidth;
                showPage(pageUnder);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // אם היינו בתוך תת-פרגמנט, נסיר אותו ונחזיר את העמוד הנוכחי מיד ללא גלילה
                if (!childfragtag.equals("")) {
                    removeChildFrag();
                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                    break;
                }

                velocityTracker.computeCurrentVelocity(1000);
                int velocityX = (int) velocityTracker.getXVelocity();
                int targetPage = currentPage;

                if (Math.abs(velocityX) > SNAP_VELOCITY) {
                    targetPage = velocityX > 0 ? currentPage - 1 : currentPage + 1;
                } else {
                    targetPage = (getScrollX() + (activeWidth / 2)) / activeWidth;
                }

                targetPage = Math.max(0, Math.min(targetPage, fragments.size() - 1));
                goToPage(targetPage, true);

                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            postInvalidate();
        }
    }

    private void showPage(int page) {
        if (page != currentPage) {
            for (int i = 0; i < fragments.size(); i++) {
                if (Math.abs(i - page) <= 1) {
                    if (!already(i)) {
                        ensureFragmentView(i);
                    }
                } else {
                    removeFragmentView(i);
                }
            }
        }
    }

    private boolean already(int index) {
        Fragment f = fragmentManager.findFragmentByTag("page" + index);
        return f != null;
    }
/*
    private void ensureFragmentView(int index) {
        if (index < 0 || index >= fragments.size()) return;

        if (!childfragtag.equals("")) {
            removeFragmentViewTag(childfragtag);
        }

        Fragment f = fragments.get(index);
        if (!already(index)) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.add(this.getId(), f, "page" + index);
            ft.commit();
            fragmentManager.executePendingTransactions();
        }

        View child = f.getView();
        if (child != null) {
            child.setTranslationX(index * activeWidth);
        }
    }
    */
    private void ensureFragmentView(int index) {
        if (index < 0 || index >= fragments.size()) return;

        if (!childfragtag.equals("")) {
            removeFragmentViewTag(childfragtag);
        }

        Fragment f = fragments.get(index);
        if (!already(index)) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.add(this.getId(), f, "page" + index);
            ft.commit();
            fragmentManager.executePendingTransactions(); // מבטיח שה-View יווצר עכשיו
        }

        View child = f.getView();
        if (child != null) {
            child.setTranslationX(index * activeWidth);
        }

        // הוסף את השורה הזו כדי לסדר את הגודל והשקיפות מיד עם היצירה
        applyTransformations(getScrollX()); 
    }
    
    private void removeFragmentView(int index) {
        Fragment f = fragmentManager.findFragmentByTag("page" + index);
        if (f != null) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.remove(f);
            ft.commit();
        }
    }

    public void childFrag(Fragment f, String tag) {
        childfragtag = tag;
        Fragment of = fragmentManager.findFragmentByTag("page" + currentPage);
        if (of != null) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.setCustomAnimations(
                R.animator.pop_enter,     // ודא שגם זה קובץ animator ולא anim
                R.animator.pop_exit,      // כנ"ל
                R.animator.pop_enter, 
                R.animator.pop_exit   
            );
            ft.remove(of);
            ft.commit();
        }
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.setCustomAnimations(
            R.animator.pop_enter,     // ודא שגם זה קובץ animator ולא anim
            R.animator.pop_exit,      // כנ"ל
            R.animator.pop_enter, 
            R.animator.pop_exit   
        );
        ft.add(this.getId(), f, tag);
        ft.commit();
        fragmentManager.executePendingTransactions();

        View child = f.getView();
        if (child != null) {
            child.setTranslationX(currentPage * activeWidth);
        }
    }

    /**
     * מסיס את ה-Child Fragment ומחזיר את הפרגמנט המקורי של העמוד הנוכחי בדיוק במקומו (ללא גלילה).
     */
    public void removeChildFrag() {
        if (!childfragtag.equals("")) {
            removeFragmentViewTag(childfragtag);
            // החזרת הפרגמנט המקורי של העמוד
            ensureFragmentView(currentPage);
            // וידוא שהגלילה נארזת בדיוק במיקום של העמוד ללא smooth scroll
            scrollTo(currentPage * activeWidth, 0);
        }
    }

    private void removeFragmentViewTag(String tag) {
        Fragment f = fragmentManager.findFragmentByTag(tag);
        if (f != null) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.setCustomAnimations(
                R.animator.pop_enter,     // ודא שגם זה קובץ animator ולא anim
                R.animator.pop_exit,      // כנ"ל
                R.animator.pop_enter, 
                R.animator.pop_exit   
            );
            ft.remove(f);
            ft.commit();
            fragmentManager.executePendingTransactions();
        }
        childfragtag = "";
    }

    public boolean gotoPlayer() {
        if (currentPage != 0) {
            goToPage(0, true);
            return true;
        } else {
            return false;
        }
    }
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        applyTransformations(l);
    }

    private void applyTransformations(int scrollX) {
        if (activeWidth == 0) return;

        for (int i = 0; i < fragments.size(); i++) {
            Fragment f = fragmentManager.findFragmentByTag("page" + i);
            if (f != null && f.getView() != null) {
                View child = f.getView();

                // חישוב המרחק הפיזי של ה-Fragment ממרכז המסך הנוכחי
                float distance = Math.abs(scrollX - (i * activeWidth));

                // הפיכת המרחק לאחוז (בין 0.0 ל-1.0)
                float fraction = distance / activeWidth;

                // חסימת האחוז שלא יעבור את 1.0 (למקרה של גלילה מהירה מדי או קצוות)
                fraction = Math.max(0f, Math.min(1f, fraction));

                // כאשר fraction = 0 (במרכז): גודל 1.0 (100%)
                // כאשר fraction = 1 (בחוץ): גודל 0.8 (80%)
                float scale = 1.0f - (0.4f * fraction);

                // כאשר fraction = 0 (במרכז): שקיפות 1.0 (מלאה)
                // כאשר fraction = 1 (בחוץ): שקיפות 0.0 (מוסתר לגמרי)
                float alpha = 1.0f - fraction;

                child.setScaleX(scale);
                child.setScaleY(scale);
                child.setAlpha(alpha);
            }
        }
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // בודקים אם הרוחב אכן השתנה (למשל בסיבוב מסך)
        if (w > 0 && oldw > 0 && w != oldw) {
            activeWidth = w; // עדכון הרוחב החדש

            // מריצים את עדכון המיקומים בתור כדי לוודא שהתצוגה מוכנה
            post(new Runnable() {
                    @Override
                    public void run() {
                        // 1. מעדכנים את הגלילה הראשית לעמוד הנוכחי
                        scrollTo(currentPage * activeWidth, 0);

                        // 2. מעדכנים את המיקום הפיזי (TranslationX) של כל הפרגמנטים הפעילים
                        for (int i = 0; i < fragments.size(); i++) {
                            Fragment f = fragmentManager.findFragmentByTag("page" + i);
                            if (f != null && f.getView() != null) {
                                f.getView().setTranslationX(i * activeWidth);
                            }
                        }

                        // 3. מעדכנים את המיקום של תת-פרגמנט (Child Fragment) אם הוא פתוח כרגע
                        if (!childfragtag.equals("")) {
                            Fragment childF = fragmentManager.findFragmentByTag(childfragtag);
                            if (childF != null && childF.getView() != null) {
                                childF.getView().setTranslationX(currentPage * activeWidth);
                            }
                        }

                        // 4. מעדכנים את האפקטים של גודל ושקיפות לפי המיקום החדש
                        applyTransformations(getScrollX());
                    }
                });
        }
    }
    public int getCurrentPage() {
        return currentPage;
    }
}
