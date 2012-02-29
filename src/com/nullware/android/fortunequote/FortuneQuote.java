package com.nullware.android.fortunequote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class FortuneQuote extends Activity {

    protected static final String TAG = "FortuneQuote";

    private static SharedPreferences preferences;
    private static QuoteData quoteData;
    private TextView textView;

    // menu
    private static final int MENU_ITEM_ABOUT_ID = Menu.FIRST;
    private static final int MENU_ITEM_PREFERENCES_ID = Menu.FIRST+1;
    private static final int MENU_ITEM_SEND_ID = Menu.FIRST+2;
    private static final int MENU_ITEM_COPY_TO_CLIPBOARD_ID = Menu.FIRST+3;
    private static final int MENU_ITEM_SHARE_APP_ID = Menu.FIRST+4;
    private static final int MENU_ITEM_EXIT_ID = Menu.FIRST+5;

    // local preference copies
    private String currentQuote;
    private String currentTopicsStr = "";
    private int[] currentTopics;
    private boolean currentRandomTopic;
    private String currentFontType = "";
    private int currentFontStyle = -1;
    private int currentFontSize = -1;
    private int currentWidgetFrequency = -1;
    private boolean currentNotificaitonActive;
    private int currentNotificationFrequency = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        if (preferences == null) preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (quoteData == null) quoteData = new QuoteData(this);
        this.textView = (TextView) findViewById(R.id.main_text);
        loadPreferences(true);
        startService(new Intent(this, NotificationService.class));
        displayStart();
        setListeners();
    }

  @Override
  protected void onResume() {
      super.onResume();
      loadPreferences(false);
      displayQuote(this.currentQuote);
  }

    @Override
    protected void onDestroy() {
        if (quoteData != null) {
            quoteData.close();
        }
        super.onDestroy();
    }

    /*
     * Menu items:
     *
     * About
     * Preferences
     * Send As Text
     * Copy To Clipboard
     * Share App
     * Exit
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.menu, menu);
        menu.add(ContextMenu.NONE, MENU_ITEM_ABOUT_ID, ContextMenu.NONE, R.string.about_label).setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(ContextMenu.NONE, MENU_ITEM_PREFERENCES_ID, ContextMenu.NONE, R.string.preferences_label).setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(ContextMenu.NONE, MENU_ITEM_SEND_ID, ContextMenu.NONE, R.string.send_label).setIcon(android.R.drawable.ic_menu_send);
        menu.add(ContextMenu.NONE, MENU_ITEM_COPY_TO_CLIPBOARD_ID, ContextMenu.NONE, R.string.copy_to_clipboard_label).setIcon(android.R.drawable.ic_menu_set_as);
        menu.add(ContextMenu.NONE, MENU_ITEM_SHARE_APP_ID, ContextMenu.NONE, R.string.share_app_label).setIcon(android.R.drawable.ic_menu_share);
        menu.add(ContextMenu.NONE, MENU_ITEM_EXIT_ID, ContextMenu.NONE, R.string.exit_label).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String currentQuote = (this.currentQuote != null) ? this.currentQuote : getResources().getString(R.string.no_quote_text);
        switch (item.getItemId()) {
        case MENU_ITEM_ABOUT_ID:
            displayAbout();
            return true;
        case MENU_ITEM_PREFERENCES_ID:
            startActivity(new Intent(this, Preferences.class));
            //Intent preferencesIntent = new Intent().setClass(this, Preferences.class);
            //startActivityForResult(preferencesIntent, 1);
            return true;
        case MENU_ITEM_SEND_ID:
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_TEXT, currentQuote)
            .setType("text/plain"), getString(R.string.send_title)));
            return true;
        case MENU_ITEM_COPY_TO_CLIPBOARD_ID:
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboardManager.setText(currentQuote);
            Toast.makeText(this, getString(R.string.copy_to_clipboard_toast), Toast.LENGTH_SHORT).show();
            return true;
        case MENU_ITEM_SHARE_APP_ID:
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app_subject))
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text))
            .setType("text/plain"), getString(R.string.share_app_title)));
            return true;
        case MENU_ITEM_EXIT_ID:
            finish();
            return true;
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        displayStart();
    }

    /**
     * Called to display the first fortune.  Determines whether app was
     * started via a notification or the normal way.
     */
    private void displayStart() {
        // handle notification
        if (getIntent() != null &&
                (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) &&
                (getIntent().getType() != null && getIntent().getType().equals("text/plain"))) {
            // cancel notification
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(NotificationService.UNIQUE_ID);
            displayQuote(getIntent().getStringExtra("text"));
        } else {
            if (this.currentQuote != null) {
                displayUpdate();
                displayQuote(this.currentQuote);
            } else {
                displayWelcome();
                displayRandomQuote();
            }
        }
    }

    /**
     * Select a random quote and display it in the main TextView.
     */
    private void displayRandomQuote() {
        assert (quoteData != null);
        String quote = quoteData.getRandomQuote(this.currentTopics, this.currentRandomTopic);
        displayQuote(quote);
    }

    /**
     * Display quote in the main TextView.  Update the font from preferences,
     * if needed.
     *
     * @param quote
     */
    private void displayQuote(String quote) {
        assert (this.textView != null);
        assert (preferences != null);
        if (quote == null) quote = getString(R.string.no_quote_text);
        this.textView.setText(quote);
        preferences.edit().putString(getString(R.string.current_quote_key), quote).commit();
        this.currentQuote = quote;
    }

    /**
     * Display welcome dialog (if first time run).
     */
    private void displayWelcome() {
        String title = getResources().getString(R.string.welcome_title);
        String text = getResources().getString(R.string.welcome_text);
        new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(title)
        .setMessage(text)
        .setPositiveButton(
                R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    /**
     * Display update log dialog (if versions have changed).
     */
    private void displayUpdate() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (preferences.getInt(getString(R.string.code_version_key), 1) != packageInfo.versionCode) {
                String title = getResources().getString(R.string.update_title);
                String text = getString(R.string.version_prefix) + packageInfo.versionName + "\n\n" +
                getResources().getString(R.string.update_text);
                new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(text)
                .setPositiveButton(
                        R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                preferences.edit().putInt(getString(R.string.code_version_key), packageInfo.versionCode).commit();
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Display about dialog.
     */
    private void displayAbout() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String title = getResources().getString(R.string.about_title);
            String text = getResources().getString(R.string.about_text) + "\n\n" +
            getString(R.string.version_prefix) + packageInfo.versionName + "\n\n" +
            getResources().getString(R.string.legal_text) + "\n\n" +
            getResources().getString(R.string.gpl_text);
            new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton(
                    R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setListeners() {
        assert (this.textView != null);
        this.textView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                displayRandomQuote();
            }
        });
//        this.textView.setOnLongClickListener(new View.OnLongClickListener() {
//            public boolean onLongClick(View v) {
//                displayRandomQuote();
//                return true;
//            }
//        });
    }

    private void loadPreferences(boolean initialize) {
        assert (preferences != null);
        // current quote
        this.currentQuote = preferences.getString(getString(R.string.current_quote_key), null);
        // topics
        String topicsDefault = getString(R.string.topics_default);
        String topicsStr = preferences.getString(getString(R.string.topics_key), topicsDefault);
        if (!this.currentTopicsStr.equals(topicsStr)) {
            String topicsStrArray[] = ListPreferenceMultiSelect.parseValue(topicsStr);
            this.currentTopicsStr = topicsStr;
            this.currentTopics = new int[topicsStrArray.length];
            for (int i = 0; i < topicsStrArray.length; i++) {
                this.currentTopics[i] = Integer.parseInt(topicsStrArray[i]);
            }
        }
        // randomTopic
        boolean randomTopicDefault = getString(R.string.randomTopic_default).equals("true");
        this.currentRandomTopic = preferences.getBoolean(getString(R.string.randomTopic_key), randomTopicDefault);
        // fontType
        String fontTypeDefault = getString(R.string.fontType_default);
        String fontType = preferences.getString(getString(R.string.fontType_key), fontTypeDefault);
        // fontStyle
        //int fontStyleDefault = Integer.parseInt(getString(R.string.fontStyle_default));
        //int fontStyle = preferences.getInt(getString(R.string.fontStyle_key), fontStyleDefault);
        String fontStyleDefault = getString(R.string.fontStyle_default);
        int fontStyle = Integer.parseInt(preferences.getString(getString(R.string.fontStyle_key), fontStyleDefault));
        // fontSize
        //int fontSizeDefault = Integer.parseInt(getString(R.string.fontSize_default));
        //int fontSize = preferences.getInt(getString(R.string.fontSize_key), fontSizeDefault);
        String fontSizeDefault = getString(R.string.fontSize_default);
        int fontSize = Integer.parseInt(preferences.getString(getString(R.string.fontSize_key), fontSizeDefault));
        // apply font settings to TextView
        if (!this.currentFontType.equals(fontType) || this.currentFontStyle != fontStyle) {
            this.currentFontType = fontType;
            this.currentFontStyle = fontStyle;
            this.textView.setTypeface(Typeface.create(this.currentFontType, this.currentFontStyle));
        }
        if (this.currentFontSize != fontSize) {
            this.currentFontSize = fontSize;
            this.textView.setTextSize(this.currentFontSize);
        }
        // widgetFrequency
        String widgetFrequencyDefault = getString(R.string.widget_frequency_default);
        int widgetFrequency = Integer.parseInt(preferences.getString(getString(R.string.widget_frequency_key), widgetFrequencyDefault));
        // reset widget service if frequency has changed
        if (this.currentWidgetFrequency != widgetFrequency) {
            this.currentWidgetFrequency = widgetFrequency;
            if (!initialize) {
                Widget.setWidgetAlarm(this);
            }
        }
        // notificationActive
        boolean notificaitonActiveDefault = getString(R.string.notification_active_default).equals("true");
        boolean notificaitonActive = preferences.getBoolean(getString(R.string.notification_active_key), notificaitonActiveDefault);
        String notificaitonFrequencyDefault = getString(R.string.notification_frequency_default);
        int notificationFrequency = Integer.parseInt(preferences.getString(getString(R.string.notification_frequency_key), notificaitonFrequencyDefault));
        if ((this.currentNotificaitonActive && !notificaitonActive) || (!this.currentNotificaitonActive && notificaitonActive) ||
                this.currentNotificationFrequency != notificationFrequency) {
            this.currentNotificaitonActive = notificaitonActive;
            this.currentNotificationFrequency = notificationFrequency;
            if (!initialize) {
                startService(new Intent(this, NotificationService.class));
            }
        }
    }

}
