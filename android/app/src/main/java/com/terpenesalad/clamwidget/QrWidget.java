package com.terpenesalad.clamwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class QrWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_qr);
        views.setOnClickPendingIntent(R.id.qr_left, open(context, QrActivity.INSTAGRAM, 1));
        views.setOnClickPendingIntent(R.id.qr_right, open(context, QrActivity.STEAM, 2));
        for (int id : ids) manager.updateAppWidget(id, views);
    }

    private static PendingIntent open(Context context, String which, int requestCode) {
        Intent intent = new Intent(context, QrActivity.class)
                .putExtra(QrActivity.EXTRA_WHICH, which)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
