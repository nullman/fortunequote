package com.nullware.android.fortunequote;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;

public class WidgetConfigure extends PreferenceActivity {

    private static final String TAG = FortuneQuote.TAG + ".WidgetConfigure";
    private static String CONFIGURE_ACTION = "android.appwidget.action.APPWIDGET_CONFIGURE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.widget_preferences, false);
        addPreferencesFromResource(R.xml.widget_preferences);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //if (keyCode == KeyEvent.KEYCODE_BACK && Integer.parseInt(Build.VERSION.SDK) < 5) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            backKeyPressed();
        }
        return(super.onKeyDown(keyCode, event));
    }

    private void backKeyPressed() {
        if (getIntent().getAction().equals(CONFIGURE_ACTION)) {
            // get the appWidgetId of the appWidget being configured
            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            if (extras != null) {
                int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                savePreferences(this, appWidgetId);
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
                appWidgetManager.updateAppWidget(appWidgetId, views);
                Intent result = new Intent();
                result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, result);
                Intent update = new Intent(this, Widget.class);
                update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                //update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                update.setAction(Widget.REFRESH_ACTION);
                sendBroadcast(update);
            }
        }
    }

    /**
     * Save preferences using {@link appWidgetId} so a widget can retrieve its
     * specific preferences.
     *
     * @param context
     * @param appWidgetId Unique ID for this widget
     */
    private static void savePreferences(Context context, int appWidgetId) {
        Log.i(TAG, "savePreferences called with appWidgetId=" + appWidgetId);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();
//        // frequency
//        int frequencyDefault = Integer.parseInt(context.getString(R.string.widget_frequency_default));
//        int frequency = preferences.getInt(context.getString(R.string.widget_frequency_key), frequencyDefault);
//        edit.putInt(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_frequency_key)), frequency);
//        Log.d(TAG, "Saved preference: " + Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_frequency_key)) + "=" + frequency);
        // topics
        String topicsDefault = context.getString(R.string.topics_default);
        String topicsStr = preferences.getString(context.getString(R.string.widget_topics_key), topicsDefault);
        edit.putString(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_topics_key)), topicsStr);
        Log.d(TAG, "Saved preference: " + Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_topics_key)) + "=" + topicsStr);
        // randomTopic
        boolean randomTopicDefault = context.getString(R.string.randomTopic_default).equals("true");
        boolean randomTopic = preferences.getBoolean(context.getString(R.string.widget_randomTopic_key), randomTopicDefault);
        edit.putBoolean(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_randomTopic_key)), randomTopic);
        Log.d(TAG, "Saved preference: " + Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_randomTopic_key)) + "=" + randomTopic);
//        // fontType
//        String fontTypeDefault = context.getString(R.string.widget_fontType_default);
//        String fontType = preferences.getString(context.getString(R.string.widget_fontType_key), fontTypeDefault);
//        edit.putString(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_fontType_key)), fontType);
//        Log.d(TAG, "Saved preference: " + Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_fontType_key)) + "=" + fontType);
//        // fontStyle
//        int fontStyleDefault = Integer.parseInt(context.getString(R.string.widget_fontStyle_default));
//        int fontStyle = preferences.getInt(context.getString(R.string.widget_fontStyle_key), fontStyleDefault);
//        edit.putInt(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_fontStyle_key)), fontStyle);
//        Log.d(TAG, "Saved preference: " + Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_fontStyle_key)) + "=" + fontStyle);
//        // fontSize
//        int fontSizeDefault = Integer.parseInt(context.getString(R.string.widget_fontSize_default));
//        int fontSize = preferences.getInt(context.getString(R.string.widget_fontSize_key), fontSizeDefault);
//        edit.putInt(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_fontSize_key)), fontSize);
//        Log.d(TAG, "Saved preference: " + Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_fontSize_key)) + "=" + fontSize);
        edit.commit();
    }

    /**
     * Delete all preferences for a given {@link appWidgetId}.
     *
     * @param context
     * @param appWidgetId Unique ID for this widget
     */
    protected static void deletePreferences(Context context, int appWidgetId) {
        Log.i(TAG, "deletePreferences called with appWidgetId=" + appWidgetId);
        SharedPreferences preferences = context.getSharedPreferences(Widget.PREFERENCES_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
//        edit.remove(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_frequency_key)));
        edit.remove(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_topics_key)));
        edit.remove(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_randomTopic_key)));
        edit.remove(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_fontType_key)));
        edit.remove(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_fontStyle_key)));
        edit.remove(Widget.getAppWidgetIdPreferenceKey(appWidgetId, context.getString(R.string.widget_fontSize_key)));
        edit.commit();
    }

}
