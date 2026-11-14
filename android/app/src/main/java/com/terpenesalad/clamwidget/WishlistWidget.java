package com.terpenesalad.clamwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WishlistWidget extends AppWidgetProvider {

    static final String ACTION_REFRESH = "com.terpenesalad.clamwidget.REFRESH";
    static final String ACTION_TOGGLE = "com.terpenesalad.clamwidget.TOGGLE";
    private static final String KEY_HIDDEN = "hidden"; // old on/off setting, read once for migration
    private static final String KEY_MODE = "mode";
    private static final String KEY_FETCHED_AT = "fetched_at";
    private static final String KEY_REFRESHING = "refreshing";
    private static final String KEY_GEN = "mode_gen";
    // Tap cycle: stats -> rat idling -> rat falls asleep -> rat wakes up -> stats
    static final String DATA = "data", IDLE = "idle", SLEEP_ENTER = "sleep_enter",
            SLEEP = "sleep", WAKE = "wake";
    private static final String PREFS = "wishlists";
    private static final String KEY_JSON = "json";
    private static final int ACCENT = 0xFFF5C04A;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        refresh(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        render(context.getApplicationContext());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            refresh(context);
        } else if (ACTION_TOGGLE.equals(intent.getAction())) {
            toggle(context.getApplicationContext());
        }
    }

    private void toggle(Context app) {
        switch (mode(app)) {
            case DATA:
                setMode(app, IDLE);
                render(app);
                break;
            case IDLE:
                // fall asleep, then switch to the sleeping loop once the clip finishes
                chain(app, SLEEP_ENTER, SLEEP, app.getResources().getInteger(R.integer.rat_sleep_enter_ms), false);
                break;
            case SLEEP_ENTER:
            case SLEEP:
                // wake up, fetch fresh numbers meanwhile, then show the stats
                chain(app, WAKE, DATA, app.getResources().getInteger(R.integer.rat_wake_ms), true);
                break;
            default: // tapped mid wake-up: skip straight to the stats
                setMode(app, DATA);
                render(app);
        }
    }

    /** Show a one-shot animation, then move on to the next mode when it ends. */
    private void chain(Context app, String now, String next, int ms, boolean fetchData) {
        final int gen = setMode(app, now);
        render(app);
        final PendingResult result = goAsync();
        new Thread(() -> {
            long start = System.currentTimeMillis();
            try {
                if (fetchData) {
                    try {
                        String json = fetch(app.getString(R.string.data_url));
                        new JSONObject(json);
                        prefs(app).edit().putString(KEY_JSON, json)
                                .putLong(KEY_FETCHED_AT, System.currentTimeMillis()).apply();
                    } catch (Exception ignored) {
                        // keep the last good numbers
                    }
                }
                long wait = ms - (System.currentTimeMillis() - start);
                if (wait > 0) Thread.sleep(wait);
            } catch (InterruptedException ignored) {
            } finally {
                if (prefs(app).getInt(KEY_GEN, 0) == gen) { // not tapped again meanwhile
                    setMode(app, next);
                    render(app);
                }
                result.finish();
            }
        }).start();
    }

    static String mode(Context context) {
        SharedPreferences p = prefs(context);
        String m = p.getString(KEY_MODE, null);
        if (m == null) m = p.getBoolean(KEY_HIDDEN, false) ? IDLE : DATA;
        return m;
    }

    private static int setMode(Context context, String mode) {
        SharedPreferences p = prefs(context);
        int gen = p.getInt(KEY_GEN, 0) + 1;
        p.edit().putString(KEY_MODE, mode).putInt(KEY_GEN, gen).commit();
        return gen;
    }

    private void refresh(Context context) {
        final Context app = context.getApplicationContext();
        prefs(app).edit().putLong(KEY_REFRESHING, System.currentTimeMillis()).commit();
        render(app);
        final PendingResult result = goAsync();
        new Thread(() -> {
            try {
                String json = fetch(app.getString(R.string.data_url));
                new JSONObject(json); // make sure it's valid before saving
                prefs(app).edit().putString(KEY_JSON, json)
                        .putLong(KEY_FETCHED_AT, System.currentTimeMillis()).apply();
            } catch (Exception ignored) {
                // keep showing the last good number
            } finally {
                prefs(app).edit().putLong(KEY_REFRESHING, 0).commit();
                render(app);
                result.finish();
            }
        }).start();
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static JSONObject cached(Context context) {
        try {
            String json = prefs(context).getString(KEY_JSON, null);
            return json == null ? null : new JSONObject(json);
        } catch (Exception e) {
            return null;
        }
    }

    static String fetch(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection)
                new URL(url + "?t=" + System.currentTimeMillis()).openConnection();
        conn.setConnectTimeout(7000);
        conn.setReadTimeout(7000);
        conn.setUseCaches(false);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    static CharSequence changeLine(JSONObject data) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        appendAccent(sb, data.optString("latest_change_text", "+0"));
        sb.append(" yesterday  ·  ");
        appendAccent(sb, data.optString("week_change_text", "+0"));
        sb.append(" this week");
        return sb;
    }

    private static void appendAccent(SpannableStringBuilder sb, String text) {
        int start = sb.length();
        sb.append(text);
        sb.setSpan(new ForegroundColorSpan(ACCENT), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // Rough sizes in dp, used to work out how many countries fit.
    private static final int CELL_HEIGHT = 17;
    private static final int MIN_CELL_WIDTH = 60;
    private static final int WIDE_MIN_WIDTH = 260;

    static void render(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, WishlistWidget.class));
        JSONObject data = cached(context);
        int ratLayout = ratLayout(mode(context));
        for (int id : ids) {
            if (ratLayout != 0) {
                RemoteViews rat = new RemoteViews(context.getPackageName(), ratLayout);
                rat.setOnClickPendingIntent(android.R.id.background, tapIntent(context));
                manager.updateAppWidget(id, rat);
                continue;
            }
            Bundle options = manager.getAppWidgetOptions(id);
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
            int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
            RemoteViews views = width >= WIDE_MIN_WIDTH
                    ? wide(context, data, width, height)
                    : compact(context, data);
            manager.updateAppWidget(id, views);
        }
    }

    private static int ratLayout(String mode) {
        switch (mode) {
            case IDLE: return R.layout.widget_rat_idle;
            case SLEEP_ENTER: return R.layout.widget_rat_sleep_enter;
            case SLEEP: return R.layout.widget_rat_sleep;
            case WAKE: return R.layout.widget_rat_wake;
            default: return 0;
        }
    }

    private static PendingIntent refreshIntent(Context context) {
        Intent tap = new Intent(context, WishlistWidget.class).setAction(ACTION_REFRESH);
        return PendingIntent.getBroadcast(context, 1, tap,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static String updatedText(Context context) {
        SharedPreferences p = prefs(context);
        // "updating…" only while a fetch is recent, so it can't get stuck if the app is killed mid-fetch
        if (System.currentTimeMillis() - p.getLong(KEY_REFRESHING, 0) < 20000) return "updating…";
        long t = p.getLong(KEY_FETCHED_AT, 0);
        if (t == 0) return "";
        return android.text.format.DateFormat.getTimeFormat(context).format(new java.util.Date(t));
    }

    private static PendingIntent tapIntent(Context context) {
        Intent tap = new Intent(context, WishlistWidget.class).setAction(ACTION_TOGGLE);
        return PendingIntent.getBroadcast(context, 0, tap,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static RemoteViews compact(Context context, JSONObject data) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        if (data != null) {
            views.setTextViewText(R.id.total, data.optString("total_text", "—"));
            views.setTextViewText(R.id.change, changeLine(data));
        }
        views.setOnClickPendingIntent(android.R.id.background, tapIntent(context));
        return views;
    }

    private static RemoteViews wide(Context context, JSONObject data, int width, int height) {
        String pkg = context.getPackageName();
        RemoteViews views = new RemoteViews(pkg, R.layout.widget_wide);
        views.setOnClickPendingIntent(android.R.id.background, tapIntent(context));
        views.setOnClickPendingIntent(R.id.refresh, refreshIntent(context));
        views.setTextViewText(R.id.updated, updatedText(context));
        views.removeAllViews(R.id.grid);
        if (data == null) {
            views.setTextViewText(R.id.change, "tap to refresh");
            return views;
        }

        String total = data.optString("total_text", "—");
        views.setTextViewText(R.id.total, total);
        views.setTextViewText(R.id.change, changeLine(data));

        JSONArray countries = data.optJSONArray("top_countries");
        int count = countries == null ? 0 : countries.length();
        if (count == 0) return views;

        // Space left for the grid after padding, header, the big number and divider.
        int bodyHeight = (height > 0 ? height : 116) - 28 - 17;
        int rows = clamp(bodyHeight / CELL_HEIGHT, 2, 8);
        int numberWidth = Math.max(total.length() * 25, 70) + 16 + 1 + 8; // number or refresh row, whichever is wider
        int gridWidth = width - 36 - numberWidth;
        int cols = clamp(gridWidth / MIN_CELL_WIDTH, 1, 6);

        int cells = rows * cols;
        boolean overflow = count > cells;
        int shown = overflow ? cells - 1 : count;
        cols = Math.min(cols, (count + rows - 1) / rows); // don't leave empty columns

        // Fill top-to-bottom, then left-to-right, so rank reads down each column.
        for (int c = 0; c < cols; c++) {
            RemoteViews column = new RemoteViews(pkg, R.layout.country_column);
            for (int r = 0; r < rows; r++) {
                int i = c * rows + r;
                if (i < shown) {
                    JSONObject entry = countries.optJSONObject(i);
                    String code = entry.optString("code", "");
                    RemoteViews cell = new RemoteViews(pkg, R.layout.country_cell);
                    cell.setTextViewText(R.id.country, flag(code) + " " + code);
                    cell.setTextViewText(R.id.count, String.valueOf(entry.optInt("adds")));
                    column.addView(R.id.column, cell);
                } else if (i == shown && overflow) {
                    RemoteViews more = new RemoteViews(pkg, R.layout.more_cell);
                    more.setTextViewText(R.id.more, "+" + (count - shown) + " more");
                    column.addView(R.id.column, more);
                }
            }
            views.addView(R.id.grid, column);
        }
        return views;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // "US" -> 🇺🇸 using regional indicator letters
    static String flag(String code) {
        if (code.length() != 2) return "";
        StringBuilder sb = new StringBuilder();
        for (char ch : code.toUpperCase().toCharArray()) {
            if (ch < 'A' || ch > 'Z') return "";
            sb.appendCodePoint(0x1F1E6 + (ch - 'A'));
        }
        return sb.toString();
    }
}
