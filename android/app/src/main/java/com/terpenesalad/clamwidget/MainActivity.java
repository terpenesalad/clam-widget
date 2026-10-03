package com.terpenesalad.clamwidget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#121212"));
        getWindow().setNavigationBarColor(Color.parseColor("#121212"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#121212"));
        int pad = (int) (32 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("CLAM Wishlists");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);

        TextView body = new TextView(this);
        body.setText("To add the widget: long-press your home screen, tap Widgets, "
                + "and find CLAM Wishlists.\n\nTap the widget any time to refresh it.");
        body.setTextColor(Color.parseColor("#8C8C8C"));
        body.setTextSize(15);
        body.setGravity(Gravity.CENTER);
        body.setPadding(0, pad / 2, 0, 0);

        root.addView(title);
        root.addView(body);
        setContentView(root);

        sendBroadcast(new Intent(this, WishlistWidget.class).setAction(WishlistWidget.ACTION_REFRESH));
    }
}
