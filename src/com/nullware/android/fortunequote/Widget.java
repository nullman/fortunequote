package com.nullware.android.fortunequote;

import java.util.Arrays;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class Widget extends AppWidgetProvider {

    private static final String TAG = FortuneQuote.TAG + ".Widget";
    protected static final String PREFERENCES_NAME = "WidgetPreferences";
    private static final String PREFERENCES_PREFIX = "appWidgetId_";
    private static final String PREFERENCES_SEPARATOR = "_";
    protected static final String REFRESH_ACTION = "com.nullware.android.fortunequote.WIDGET_REFRESH";
    private static final int MINUTE = 1000 * 60;

    private static PendingIntent alarmPendingIntent;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.i(TAG, "onEnabled called");
        setWidgetAlarm(context);
    }

    @Override
    public void onDisabled(Context context) {
        Log.i(TAG, "onDisabled called");
        cancelWidgetAlarm(context);
        super.onDisabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.i(TAG, "onUpdate called with appWidgetIds=" + Arrays.toString(appWidgetIds));
        // start widget service
        context.startService(new Intent(context, WidgetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds));
        // set widget alarm
        setWidgetAlarm(context);
//        // check all widgets
//        appWidgetManager = AppWidgetManager.getInstance(context);
//        final int[] allAppWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, Widget.class));
//        for (int i = 0; i < allAppWidgetIds.length; i++) {
//            if (appWidgetIds.length != allAppWidgetIds.length || appWidgetIds[i] != allAppWidgetIds[i]) {
//                setWidgetAlarm(context);
//                break;
//            }
//        }
    }

    /**
     * Process widget window clicks and alarm triggers.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(REFRESH_ACTION)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
                int[] appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                Intent update = new Intent(context, WidgetService.class);
                if (appWidgetIds != null) {
                    Log.d(TAG, "onReceive called with appWidgetIds=" + Arrays.toString(appWidgetIds));
                    update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                    context.startService(update);
                } else if (appWidgetId != 0) {
                    Log.d(TAG, "onReceive called with appWidgetId=" + appWidgetId);
                    update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    context.startService(update);
                }
            }
            // set widget alarm
            setWidgetAlarm(context);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.i(TAG, "onDeleted called with appWidgetIds=" + Arrays.toString(appWidgetIds));
        for (int appWidgetId : appWidgetIds) {
            WidgetConfigure.deletePreferences(context, appWidgetId);
        }
    }

    /**
     * Set an alarm to update widgets.
     *
     * @param context
     */
    protected static void setWidgetAlarm(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        //int frequencyDefault = Integer.parseInt(context.getString(R.string.widget_frequency_default));
        //long frequency = preferences.getInt(context.getString(R.string.widget_frequency_key), frequencyDefault) * MINUTE;
        String frequencyDefault = context.getString(R.string.widget_frequency_default);
        long frequency = Integer.parseInt(preferences.getString(context.getString(R.string.widget_frequency_key), frequencyDefault)) * MINUTE;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmPendingIntent = getWidgetAlarmPendingIntent(context);
        if (alarmPendingIntent != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + frequency, frequency, alarmPendingIntent);
        }
    }

    private static PendingIntent getWidgetAlarmPendingIntent(Context context) {
        PendingIntent pendingIntent = null;
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, Widget.class));
        if (appWidgetIds.length > 0) {
            Intent intent = new Intent(context, Widget.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            //intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.setAction(REFRESH_ACTION);
            pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return pendingIntent;
    }

    private static void cancelWidgetAlarm(Context context) {
        if (alarmPendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(alarmPendingIntent);
        }
    }

    protected static String getAppWidgetIdPreferenceKey(int appWidgetId, String key) {
        return PREFERENCES_PREFIX + appWidgetId + PREFERENCES_SEPARATOR + key;
    }

}
