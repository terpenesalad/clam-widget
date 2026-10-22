package com.terpenesalad.clamwidget;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shows one QR code large on a black screen at full brightness. Tap anywhere to close. */
public class QrActivity extends Activity {

    static final String EXTRA_WHICH = "which";
    static final String INSTAGRAM = "instagram";
    static final String STEAM = "steam";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean steam = STEAM.equals(getIntent().getStringExtra(EXTRA_WHICH));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1f; // max brightness while open makes scanning easier
        getWindow().setAttributes(lp);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        int screen = Math.min(getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        int size = (int) (screen * 0.78f);
        int pad = dp(20);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setOnClickListener(v -> finish());

        ImageView qr = new ImageView(this);
        qr.setImageResource(steam ? R.drawable.qr_steam : R.drawable.qr_instagram);
        qr.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qr.setPadding(pad, pad, pad, pad);
        GradientDrawable tile = new GradientDrawable();
        tile.setColor(Color.WHITE);
        tile.setCornerRadius(dp(22));
        qr.setBackground(tile);
        qr.setContentDescription(steam ? "Steam QR code" : "Instagram QR code");
        root.addView(qr, new LinearLayout.LayoutParams(size, size));

        TextView title = text(steam ? "STEAM" : "INSTAGRAM", 12, "#8C8C8C");
        title.setLetterSpacing(0.18f);
        title.setPadding(0, dp(28), 0, 0);
        root.addView(title);

        TextView name = text(steam ? "CLAM" : "@clamtheratgame", 20, "#FFFFFF");
        name.setPadding(0, dp(6), 0, 0);
        root.addView(name);

        TextView hint = text("Tap anywhere to close", 12, "#5C5C5C");
        hint.setPadding(0, dp(40), 0, 0);
        root.addView(hint);

        setContentView(root);
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private TextView text(String s, int sp, String color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(Color.parseColor(color));
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
