package com.terpenesalad.clamwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.RemoteViews;

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

    static void render(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        JSONObject data = cached(context);
        if (data != null) {
            views.setTextViewText(R.id.total, data.optString("total_text", "—"));
            views.setTextViewText(R.id.change, changeLine(data));
        }

        Intent tap = new Intent(context, WishlistWidget.class).setAction(ACTION_REFRESH);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, tap,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(android.R.id.background, pending);

        AppWidgetManager.getInstance(context)
                .updateAppWidget(new ComponentName(context, WishlistWidget.class), views);
    }
}
