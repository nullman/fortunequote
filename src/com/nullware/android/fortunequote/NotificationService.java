package com.nullware.android.fortunequote;

import java.util.Arrays;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class NotificationService extends Service {

    private static final String TAG = FortuneQuote.TAG + ".NotificationService";
    protected static final int UNIQUE_ID = R.id.main_text;

    private static SharedPreferences preferences;
    private static QuoteData quoteData;
    private static NotificationManager notificationManager;
    private static ConditionVariable notificationPeriod;
    private static Thread notificationThread;
    private static final int notificationThreadTimeOut = 1000; // one second
    private static final int MINUTE = 1000 * 60;

    @Override
    public void onCreate() {
        if (preferences == null) preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (quoteData == null) quoteData = new QuoteData(this);
        if (notificationManager == null) notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationPeriod == null) notificationPeriod = new ConditionVariable(false);
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

    @Override
    public void onDestroy() {
        // stop any running periodic notification
        stopPeriodicNotification();
        super.onDestroy();
    }

    public synchronized void handleIntent(Intent intent) {
        Log.d(TAG, "handleIntent called");
        boolean notificationActiveDefault = getString(R.string.notification_active_default).equals("true");
        boolean notificationActive = preferences.getBoolean(getString(R.string.notification_active_key), notificationActiveDefault);
        if (notificationActive) {
            // handle system startup intent
            boolean notificationStartupDefault = getString(R.string.notification_startup_default).equals("true");
            boolean notificationStartup = preferences.getBoolean(getString(R.string.notification_startup_key), notificationStartupDefault);
            if (intent != null && intent.getBooleanExtra("onStartup", false) == true && notificationStartup) {
                sendNotification();
            }
            // start periodic notification
            periodicNotification();
        } else {
            // stop any running periodic notification
            stopPeriodicNotification();
        }
    }

    private synchronized void periodicNotification() {
        assert (preferences != null);
        Log.d(TAG, "periodicNotification called");
        // only want one periodic notification thread running
        if (notificationThread == null) {
            Log.d(TAG, "starting periodic notification");
            notificationThread = new Thread(new Runnable(){
                synchronized public void run() {
                    boolean done = false;
                    while (!done) {
                        boolean notificationActiveDefault = getString(R.string.notification_active_default).equals("true");
                        boolean notificationActive = preferences.getBoolean(getString(R.string.notification_active_key), notificationActiveDefault);
                        //int notificationFrequencyDefault = Integer.parseInt(getString(R.string.notification_frequency_default));
                        //int notificationFrequency = preferences.getInt(getString(R.string.notification_frequency_key), notificationFrequencyDefault);
                        String notificationFrequencyDefault = getString(R.string.notification_frequency_default);
                        int notificationFrequency = Integer.parseInt(preferences.getString(getString(R.string.notification_frequency_key), notificationFrequencyDefault));
                        if (!notificationActive || notificationPeriod.block(notificationFrequency * MINUTE)) {
                            done = true;
                        } else {
                            sendNotification();
                        }
                    }
                }
            });
            notificationThread.start();
        }
    }

    private synchronized static void stopPeriodicNotification() {
        Log.d(TAG, "stopPeriodicNotification called");
        if (notificationThread != null && notificationThread.isAlive()) {
            Log.d(TAG, "stopping running notification...");
            try {
                notificationPeriod.open();
                notificationManager.cancel(UNIQUE_ID);
                notificationThread.interrupt();
                // give the thread some time to close
                int i = 0;
                int incr = notificationThreadTimeOut/10;
                while (notificationThread.isAlive() && i < notificationThreadTimeOut) {
                    Thread.sleep(incr);
                    i += incr;
                }
                if (notificationThread.isAlive()) {
                    String err = "Error stopping periodic notification service";
                    Log.e(TAG, err);
                    throw new InterruptedException(err);
                }
                notificationThread = null;
                Log.d(TAG, "notification stopped");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendNotification() {
        Log.d(TAG, "sendNotification called");
        // topics
        String topicsDefault = getString(R.string.topics_default);
        String topicsStr = preferences.getString("topics", topicsDefault);
        Log.d(TAG, "Loaded preference: " + getString(R.string.topics_key) + "=" + topicsStr);
        String topicsStrArray[] = ListPreferenceMultiSelect.parseValue(topicsStr);
        int[] topics = new int[topicsStrArray.length];
        for (int i = 0; i < topicsStrArray.length; i++) {
            topics[i] = Integer.parseInt(topicsStrArray[i]);
        }
        // randomTopic
        boolean randomTopicDefault = getString(R.string.randomTopic_default).equals("true");
        boolean randomTopic = preferences.getBoolean("randomTopic", randomTopicDefault);
        Log.d(TAG, "Loaded preference: " + getString(R.string.randomTopic_key) + "=" + randomTopic);
        // get quote
        Log.d(TAG, "getRandomQuote called with topics=" + Arrays.toString(topics) + ", randomTopic=" + randomTopic);
        String quote = quoteData.getRandomQuote(topics, randomTopic);
        // setup notification
        Notification notification = new Notification(R.drawable.ic_launcher, quote, System.currentTimeMillis());
        // create a PendingIntent to launch main FortuneQuote activity if user selects notification
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra("text", quote);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT + PendingIntent.FLAG_ONE_SHOT);
        notification.setLatestEventInfo(this, getText(R.string.notification_title), quote, pendingIntent);
        notificationManager.notify(UNIQUE_ID, notification);
     }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
