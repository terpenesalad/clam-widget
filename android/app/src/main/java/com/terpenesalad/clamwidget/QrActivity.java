package com.terpenesalad.clamwidget;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Choreographer;
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

    static final String EXTRA_WHICH = "which";
    static final String INSTAGRAM = "instagram";
    static final String STEAM = "steam";

    // Full-screen idle: its own 60fps frames (rat_qr_*), picked by elapsed time on every screen refresh.
    private static final int FPS = 60;
    private ImageView rat;
    private int[] frames;
    private int shown = -1;
    private long start;
    private boolean running;
    private final Choreographer.FrameCallback tick = new Choreographer.FrameCallback() {
        @Override public void doFrame(long now) {
            if (!running) return;
            int f = (int) (((now - start) / 1_000_000L) * FPS / 1000 % frames.length);
            if (f != shown) { rat.setImageResource(frames[f]); shown = f; }
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

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

        if (!steam) {
            TextView name = text("@clamtheratgame", 20, "#FFFFFF");
            name.setPadding(0, dp(24), 0, 0);
            root.addView(name);
        }

        // the rat doing her idle, using the same frames as the widget
        java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
        for (int i = 0; ; i++) {
            int id = getResources().getIdentifier(String.format(java.util.Locale.ROOT, "rat_qr_%03d", i),
                    "drawable", getPackageName());
            if (id == 0) break;
            ids.add(id);
        }
        frames = new int[ids.size()];
        for (int i = 0; i < frames.length; i++) frames[i] = ids.get(i);
        rat = new ImageView(this);
        rat.setScaleType(ImageView.ScaleType.FIT_CENTER);
        rat.setContentDescription("CLAM the rat");
        int ratWidth = (int) (screen * 0.95f);
        LinearLayout.LayoutParams ratLp = new LinearLayout.LayoutParams(ratWidth, ratWidth * 298 / 540);
        ratLp.topMargin = dp(12);
        root.addView(rat, ratLp);
        // her body sits ~32% across the frame (the tail fills the rest), so shift her right to
        // centre the body; the tail runs off the edge of the screen
        rat.setTranslationX(ratWidth * (0.5f - 0.32f));
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
        if (frames != null && frames.length > 0) {
            running = true;
            start = System.nanoTime();
            Choreographer.getInstance().postFrameCallback(tick);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        Choreographer.getInstance().removeFrameCallback(tick);
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
