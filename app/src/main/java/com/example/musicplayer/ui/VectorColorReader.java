package com.example.musicplayer.ui;
import org.xmlpull.v1.XmlPullParser;
import android.content.Context;
import android.content.res.XmlResourceParser;
import com.example.musicplayer.LogUtil;
import android.util.TypedValue;
import android.os.Build;
import android.graphics.Color;
import com.example.musicplayer.R;

public final class VectorColorReader {

    private static final String ANDROID_NS =
    "http://schemas.android.com/apk/res/android";

    private VectorColorReader() {
    }

    public static boolean containsFillColor(
        Context context,
        int drawableId,
        int wantedColor
    ) {
        XmlResourceParser parser = null;

        try {
            parser = context.getResources()
                .getXml(drawableId);

            int event;

            while ((event = parser.next())
                   != XmlPullParser.END_DOCUMENT) {

                if (event != XmlPullParser.START_TAG) {
                    continue;
                }
                if ("vector".equals(parser.getName())) {
                LogUtil.logToFile(parser.getAttributeValue(ANDROID_NS,"tint")+"tint="+parser.getAttributeResourceValue(
                                          ANDROID_NS,
                                          "tint",
                                          0
                                      )+"w="+wantedColor);
                    if (parser.getAttributeValue(ANDROID_NS,"tint").equals("?"+ wantedColor)) {
                        return true;
                    }
                }
                if (!"path".equals(parser.getName())) {
                    continue;
                }

                int fillColor =
                    getFillColor(
                    context,
                    parser
                );

                if (fillColor == wantedColor) {
                    return true;
                }
            }

        } catch (Exception e) {
            LogUtil.logToFile(e);

        } finally {
            if (parser != null) {
                parser.close();
            }
        }

        return false;
    }

    private static int getFillColor(
        Context context,
        XmlResourceParser parser
    ) {
        String rawValue =
            parser.getAttributeValue(
            ANDROID_NS,
            "fillColor"
        );

        if (rawValue == null) {
            return Integer.MIN_VALUE;
        }

        /*
         * fillColor="#FFFFFFFF"
         */
        if (rawValue.startsWith("#")) {
            try {
                return Color.parseColor(rawValue);
            } catch (Exception e) {
                return Integer.MIN_VALUE;
            }
        }

        /*
         * fillColor="@color/..."
         */
        int resourceId =
            parser.getAttributeResourceValue(
            ANDROID_NS,
            "fillColor",
            0
        );

        if (resourceId != 0) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    return context.getResources()
                        .getColor(
                        resourceId,
                        context.getTheme()
                    );
                }

                return context.getResources()
                    .getColor(resourceId);

            } catch (Exception e) {
                return Integer.MIN_VALUE;
            }
        }

        /*
         * fillColor="?colorBackItem"
         */
        if (rawValue.startsWith("?")) {
            return resolveColorAttribute(
                context,
                R.attr.colorBackItem,
                Integer.MIN_VALUE
            );
        }

        return Integer.MIN_VALUE;
    }

    private static int resolveColorAttribute(
        Context context,
        int attrId,
        int fallback
    ) {
        TypedValue value = new TypedValue();

        boolean found =
            context.getTheme().resolveAttribute(
            attrId,
            value,
            true
        );

        if (!found) {
            return fallback;
        }

        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
            && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        }

        if (value.resourceId != 0) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    return context.getResources()
                        .getColor(
                        value.resourceId,
                        context.getTheme()
                    );
                }

                return context.getResources()
                    .getColor(value.resourceId);

            } catch (Exception e) {
                return fallback;
            }
        }

        return fallback;
    }
}

