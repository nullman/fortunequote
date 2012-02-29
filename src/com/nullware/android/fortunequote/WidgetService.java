package com.nullware.android.fortunequote;

import java.util.Arrays;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Service that displays a new random quote in all widgets.
 */
public class WidgetService extends Service {

    private static final String TAG = FortuneQuote.TAG + ".WidgetService";

    private static SharedPreferences preferences;
    private static QuoteData quoteData;

    @Override
    public void onCreate() {
        if (preferences == null) preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (quoteData == null) quoteData = new QuoteData(this);
//        if (widgetPeriod == null) widgetPeriod = new ConditionVariable(false);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        handleIntent(intent);
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        handleIntent(intent);
//        return START_NOT_STICKY;
//    }

    public synchronized void handleIntent(Intent intent) {
        Log.d(TAG, "handleIntent called");
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS) != null) {
                updateWidget(this, extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS));
            } else if (extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 0) != 0) {
                updateWidget(this, extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID));
            }
        }
        // do not keep running
        stopSelf();
    }

//    /**
//     * Update all widgets.
//     */
//    private void updateAllWidgets() {
//        Log.d(TAG, "updateAllWidgets called");
//        Context context = getApplicationContext();
//        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
//        final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, Widget.class));
//        if (appWidgetIds != null && appWidgetIds.length > 0) {
//            updateWidget(context, appWidgetIds);
//        }
//    }

    /**
     * Update a group of widgets.
     *
     * @param context
     * @param appWidgetIds List of widget IDs to update
     */
    private void updateWidget(Context context, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];
            updateWidget(context, appWidgetId);
        }
    }

    /**
     * Update a single widget.
     *
     * @param context
     * @param appWidgetId The widget ID of the widget to update
     */
    private void updateWidget(Context context, int appWidgetId) {
        assert (preferences != null);
        Log.d(TAG, "updateWidget called with appWidgetId=" + appWidgetId);
        if (quoteData == null) quoteData = new QuoteData(context);
        // get preferences for this appWidgetId
        // topics
        String topicsDefault = context.getString(R.string.topics_default);
        String topicsStr = preferences.getString(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_topics_key)), topicsDefault);
        Log.d(TAG, "Loaded preference: " + Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_topics_key)) + "=" + topicsStr);
        String topicsStrArray[] = ListPreferenceMultiSelect.parseValue(topicsStr);
        int[] topics = new int[topicsStrArray.length];
        for (int j = 0; j < topicsStrArray.length; j++) {
            topics[j] = Integer.parseInt(topicsStrArray[j]);
        }
        // randomTopic
        boolean randomTopicDefault = context.getString(R.string.randomTopic_default).equals("true");
        boolean randomTopic = preferences.getBoolean(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_randomTopic_key)), randomTopicDefault);
        Log.d(TAG, "Loaded preference: " + Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_randomTopic_key)) + "=" + randomTopic);
//            // fontType
//            String fontTypeDefault = context.getString(R.string.widget_fontType_default);
//            String fontType = preferences.getString(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_fontType_key)), fontTypeDefault);
//            // fontStyle
//            int fontStyleDefault = Integer.parseInt(context.getString(R.string.widget_fontStyle_default));
//            int fontStyle = preferences.getInt(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_fontStyle_key)), fontStyleDefault);
//            // fontSize
//            int fontSizeDefault = Integer.parseInt(context.getString(R.string.widget_fontSize_default));
//            int fontSize = preferences.getInt(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_fontSize_key)), fontSizeDefault);
        Log.d(TAG, "getRandomQuote called with topics=" + Arrays.toString(topics) + ", randomTopic=" + randomTopic);
        // get a quote
        String quote = quoteData.getRandomQuote(topics, randomTopic);
        // update quote in view
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setTextViewText(R.id.widget_text, quote);
        // create on click pending intent
        Intent intent = new Intent(this, Widget.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        //intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.setAction(Widget.REFRESH_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_text, pendingIntent);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        manager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
