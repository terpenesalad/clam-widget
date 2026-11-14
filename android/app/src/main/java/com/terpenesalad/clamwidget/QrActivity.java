package com.terpenesalad.clamwidget;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shows one QR code large on a black screen at full brightness, with CLAM above and the rat
 *  idling below. Tap anywhere to close. */
public class QrActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ImageView rat;
    private int[] frames;
    private int frame;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            rat.setImageResource(frames[frame]);
            frame = (frame + 1) % frames.length;
            handler.postDelayed(this, 50); // 20fps, same as the widget's idle
        }
    };

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
        int size = (int) (screen * 0.72f);
        int pad = dp(20);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setOnClickListener(v -> finish());

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.clam_logo);
        logo.setAdjustViewBounds(true);
        logo.setContentDescription("CLAM");
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(30));
        logoLp.bottomMargin = dp(32);
        root.addView(logo, logoLp);

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

        // the rat doing her idle, using the same frames as the widget
        java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
        for (int i = 0; ; i++) {
            int id = getResources().getIdentifier(String.format(java.util.Locale.ROOT, "rat_idle_%03d", i),
                    "drawable", getPackageName());
            if (id == 0) break;
            ids.add(id);
        }
        frames = new int[ids.size()];
        for (int i = 0; i < frames.length; i++) frames[i] = ids.get(i);
        rat = new ImageView(this);
        rat.setScaleType(ImageView.ScaleType.FIT_CENTER);
        rat.setContentDescription("CLAM the rat");
        int ratWidth = (int) (screen * 0.7f);
        LinearLayout.LayoutParams ratLp = new LinearLayout.LayoutParams(ratWidth, ratWidth * 199 / 360);
        ratLp.topMargin = dp(20);
        root.addView(rat, ratLp);
        if (frames.length > 0) rat.setImageResource(frames[0]);

        TextView hint = text("Tap anywhere to close", 12, "#5C5C5C");
        hint.setPadding(0, dp(16), 0, 0);
        root.addView(hint);

        setContentView(root);
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (frames != null && frames.length > 0) handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
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
