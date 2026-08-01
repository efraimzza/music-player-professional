package com.example.musicplayer;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;
import android.widget.LinearLayout;
import android.widget.Button;
import android.view.View;

public class debug extends Activity {

    private static final Map<String, String> exceptionMap = new HashMap<String, String>();
    {
        exceptionMap.put("StringIndexOutOfBoundsException", "Invalid string operation\n");
        exceptionMap.put("IndexOutOfBoundsException", "Invalid list operation\n");
        exceptionMap.put("ArithmeticException", "Invalid arithmetical operation\n");
        exceptionMap.put("NumberFormatException", "Invalid toNumber block operation\n");
        exceptionMap.put("ActivityNotFoundException", "Invalid intent operation\n");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String errorMessage = "";
        if (intent != null) {
            errorMessage = intent.getStringExtra("error");
        }

        final SpannableStringBuilder formattedMessage = new SpannableStringBuilder();

        if (errorMessage != null && !errorMessage.isEmpty()) {
            // Split lines for parsing
            String[] split = errorMessage.split("\n");
            String firstLine = split[0].trim();

            // Extract only simple exception class name (no package prefix)
            String exceptionType = firstLine;
            int dotIndex = firstLine.lastIndexOf('.');
            if (dotIndex != -1 && dotIndex < firstLine.length() - 1) {
                exceptionType = firstLine.substring(dotIndex + 1);
            }

            // Remove trailing message after colon if present
            int colonIndex = exceptionType.indexOf(':');
            if (colonIndex != -1) {
                exceptionType = exceptionType.substring(0, colonIndex).trim();
            }

            // Lookup friendly message
            String friendlyMessage = exceptionMap.getOrDefault(exceptionType, "");

            if (!friendlyMessage.isEmpty()) {
                formattedMessage.append(friendlyMessage).append("\n");
            }

            // Append the full error content (stack trace)
            formattedMessage.append(errorMessage);
        } else {
            formattedMessage.append("No error message available.");
        }

        setTitle(getTitle() + " קרס");

        TextView errorView = new TextView(this);
        errorView.setText(formattedMessage);
        errorView.setTextIsSelectable(true);
        errorView.setTypeface(Typeface.SANS_SERIF);
        errorView.setPadding(32, 32, 32, 32);
        LinearLayout linl=new LinearLayout(this);
        linl.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);
        errorView.setLayoutParams(lp);
        linl.addView(errorView);
        Button bu=new Button(this);
       // bu.setText(R.string.share);
        bu.setOnClickListener(new Button.OnClickListener(){

                @Override
                public void onClick(View p1) {
                   // Utils.shareText(debug.this, getTitle().toString(), formattedMessage.toString());
                }
            });
        lp=new LinearLayout.LayoutParams(-2,-2);
        bu.setLayoutParams(lp);
        linl.addView(bu);
        // Add scroll support (both directions)
        HorizontalScrollView hscroll = new HorizontalScrollView(this);
        ScrollView vscroll = new ScrollView(this);
        vscroll.addView(linl);
        hscroll.addView(vscroll);

        setContentView(hscroll);
    }
    


}
