package com.example.geyerk1.inspect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

public class startServiceOnBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int initialDay = preferences.getInt("date started", 0);
        int currentDay = cal.get(Calendar.DAY_OF_YEAR);

        Log.i("From screenService", "current hour" + currentDay + " : from prefs: " + initialDay);

        if (currentDay > initialDay) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Intent intent1 = new Intent(context, notificationService.class);
                context.startService(intent1);
            }
        } else {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Intent initiateServices = new Intent(context, phonerestarted.class);
                context.startService(initiateServices);
            }
        }

    }
}

