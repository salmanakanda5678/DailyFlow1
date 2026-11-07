package com.dailyroutine.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dailyroutine.database.DatabaseHelper;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            PreferenceManager prefs = PreferenceManager.getInstance(context);
            DatabaseHelper db = DatabaseHelper.getInstance(context);

            if (prefs.isAppBlockerEnabled() || db.getActiveFocusTask() != null) {
                AppBlockerService.start(context);
            }
        }
    }
}
