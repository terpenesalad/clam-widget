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
        }
    }

    private void refresh(Context context) {
        final Context app = context.getApplicationContext();
        render(app);
        final PendingResult result = goAsync();
        new Thread(() -> {
            try {
                String json = fetch(app.getString(R.string.data_url));
                new JSONObject(json); // make sure it's valid before saving
                prefs(app).edit().putString(KEY_JSON, json).apply();
            } catch (Exception ignored) {
                // keep showing the last good number
            } finally {
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
        for (int id : ids) {
            Bundle options = manager.getAppWidgetOptions(id);
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
            int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
            RemoteViews views = width >= WIDE_MIN_WIDTH
                    ? wide(context, data, width, height)
                    : compact(context, data);
            manager.updateAppWidget(id, views);
        }
    }

    private static PendingIntent tapIntent(Context context) {
        Intent tap = new Intent(context, WishlistWidget.class).setAction(ACTION_REFRESH);
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
        int numberWidth = total.length() * 27 + 16 + 1 + 8;
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
