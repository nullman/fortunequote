package com.nullware.android.fortunequote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

public class SystemStartup extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
            // start notification service
            context.startService(new Intent(context, NotificationService.class).putExtra("onStartup", true));
        }
    }

}
