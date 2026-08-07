package com.example.musicplayer.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import android.widget.ImageView;
import com.example.musicplayer.LogUtil;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.graphics.PorterDuff;
import com.example.musicplayer.R;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.EditText;

public final class RuntimeInflater {

    private RuntimeInflater() {
    }

    public static Hook install(
        LayoutInflater inflater,
        ThemePalette palette
    ) {
        Hook hook = new Hook(palette);

        /*
         * אי אפשר להחליף Factory שכבר הותקן
         * בלי לשבור אותו או להשתמש ב-reflection.
         */
        if (inflater.getFactory2() == null
            && inflater.getFactory() == null) {

            inflater.setFactory2(hook);
        }

        return hook;
    }

    public static class Hook implements LayoutInflater.Factory2 {

        private ThemePalette palette;

        private final Map<String, Constructor<? extends View>>
        constructors =
        new HashMap<String, Constructor<? extends View>>();

        public Hook(ThemePalette palette) {
            this.palette = palette;
        }

        public void setPalette(ThemePalette palette) {
            this.palette = palette;
        }

        @Override
        public View onCreateView(
            View parent,
            String name,
            Context context,
            AttributeSet attrs
        ) {
            return createAndApply(
                name,
                context,
                attrs
            );
        }

        @Override
        public View onCreateView(
            String name,
            Context context,
            AttributeSet attrs
        ) {
            return createAndApply(
                name,
                context,
                attrs
            );
        }

        private View createAndApply(
            String name,
            Context context,
            AttributeSet attrs
        ) {
            View view = createView(
                name,
                context,
                attrs
            );

            if (view != null) {
                applyPalette(
                    attrs,
                    view,
                    palette,
                    context
                );
            }

            /*
             * null אומר ל-LayoutInflater להמשיך
             * עם הטיפול הרגיל שלו עבור tags מיוחדים.
             */
            return view;
        }

        private View createView(
            String name,
            Context context,
            AttributeSet attrs
        ) {
            String[] candidates;

            if (name.indexOf('.') >= 0) {
                candidates = new String[] {
                    name
                };
            } else {
                candidates = new String[] {
                    "android.widget." + name,
                    "android.webkit." + name,
                    "android.app." + name,
                    "android.view." + name
                };
            }

            for (int i = 0; i < candidates.length; i++) {
                View view = createWithConstructor(
                    candidates[i],
                    context,
                    attrs
                );

                if (view != null) {
                    return view;
                }
            }

            return null;
        }

        private View createWithConstructor(
            String className,
            Context context,
            AttributeSet attrs
        ) {
            try {
                Constructor<? extends View> constructor;

                synchronized (constructors) {
                    constructor = constructors.get(className);

                    if (constructor == null) {
                        Class<?> type =
                            Class.forName(className);

                        if (!View.class.isAssignableFrom(type)) {
                            return null;
                        }

                        @SuppressWarnings("unchecked")
                            Constructor<? extends View> found =
                            (Constructor<? extends View>)
                            type.getConstructor(
                            Context.class,
                            AttributeSet.class
                        );

                        constructor = found;
                        constructors.put(
                            className,
                            constructor
                        );
                    }
                }

                return constructor.newInstance(
                    context,
                    attrs
                );

            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private static void applyPalette(
        AttributeSet attrs,
        View view,
        ThemePalette palette,
        Context context
    ) {
        if (palette == null) {
            return;
        }

        /*
         * כל TextView שנוצר יקבל את colorText שלך.
         * זה כולל Button, EditText, CheckBox וכו'
         * כי הם יורשים מ-TextView או CompoundButton.
         */
        if (view instanceof TextView) {
            TextView textView =
                (TextView) view;

            textView.setTextColor(
                palette.text
            );

            textView.setHintTextColor(
                palette.text
            );

            textView.setLinkTextColor(
                palette.text
            );
        }
        if (view instanceof EditText) {
            EditText editText =
                (EditText) view;

            editText.setTextColor(
                palette.text
            );

            editText.setHintTextColor(
                palette.text
            );

            editText.setLinkTextColor(
                palette.text
            );
        }
        /*
         * Buttons יקבלו colorBackItem כרקע
         * ו-colorText כצבע טקסט.
         */
        if (view instanceof Button) {
            RuntimeBackground.apply(
                context,
                view,
                palette.backItem,
                RuntimeBackground.getPressedColor(
                    palette.backItem
                ),
                28f,
                32f
            );
        }

   

        if (view instanceof ImageView) {
            ImageView imageView =
                (ImageView) view;

            int drawableId =
                getImageResourceId(attrs);

            if (drawableId != 0) {
                try {
                    String resourceName =
                        context.getResources()
                        .getResourceName(drawableId);

                    String resourceType =
                        context.getResources()
                        .getResourceTypeName(drawableId);

                    LogUtil.logToFile(
                        "drawableId=" + drawableId
                        + ", name=" + resourceName
                        + ", type=" + resourceType
                    );

                    Drawable drawable =
                        imageView.getDrawable();

                    if (drawable instanceof VectorDrawable) {
                        LogUtil.logToFile(
                            "This is VectorDrawable"
                        );

                        applyVectorTintIfMatches(
                            imageView,
                            drawableId,
                            context
                        );
                    }

                } catch (Exception e) {
                    LogUtil.logToFile(e);
                }
            }
        }

       

       
    
        /*
         * Accent עבור CheckBox / RadioButton / Switch.
         */
        if (Build.VERSION.SDK_INT >= 21
            && view instanceof CompoundButton) {

            CompoundButton button =
                (CompoundButton) view;

            button.setButtonTintList(
                ColorStateList.valueOf(
                    palette.accent
                )
            );
        }
    }
    private static void applyVectorTintIfMatches(
        ImageView imageView,
        int drawableId,
        Context context
    ) {
        int oldBackItem =
            resolveColorAttribute(
            context,
            R.attr.colorBackItem,
            Color.TRANSPARENT
        );
        int controlNormal =
          
            android.R.attr.colorControlNormal;
        boolean found =
            VectorColorReader.containsFillColor(
            context,
            drawableId,
            controlNormal
        );

        if (!found) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 21) {
            imageView.setImageTintList(
                ColorStateList.valueOf(
                    ThemeManager
                    .getCurrentPalette(context)
                    .backItem
                )
            );
        } else {
            imageView.setColorFilter(
                ThemeManager
                .getCurrentPalette(context)
                .backItem,
                PorterDuff.Mode.SRC_IN
            );
        }
    }
    

    private static int resolveColorAttribute(
        Context context,
        int attribute,
        int fallback
    ) {
        TypedValue value = new TypedValue();

        boolean found =
            context.getTheme().resolveAttribute(
            attribute,
            value,
            true
        );

        if (!found) {
            return fallback;
        }

        if (value.resourceId != 0) {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                return context.getResources().getColor(
                    value.resourceId,
                    context.getTheme()
                );
            }

            return context.getResources().getColor(
                value.resourceId
            );
        }

        return value.data;
    }
    private static final String ANDROID_NS =
    "http://schemas.android.com/apk/res/android";

    private static final String AUTO_NS =
    "http://schemas.android.com/apk/res-auto";

    private static int getImageResourceId(
        AttributeSet attrs
    ) {
        if (attrs == null) {
            return 0;
        }

        /*
         * android:src="@drawable/..."
         */
        int id = attrs.getAttributeResourceValue(
            ANDROID_NS,
            "src",
            0
        );

        if (id != 0) {
            return id;
        }

        /*
         * app:srcCompat="@drawable/..."
         *
         * זה רלוונטי רק אם קיים אצלך attr בשם srcCompat.
         */
        return attrs.getAttributeResourceValue(
            AUTO_NS,
            "srcCompat",
            0
        );
    }
    
}

