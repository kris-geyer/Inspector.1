package com.example.geyerk1.inspect;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

public class screenService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if(!preferences.getBoolean("apps documented", false)){
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("apps documented", true);
            editor.apply();

            startService(new Intent(this, installedApps.class));
        }

        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenFilter.addAction(Intent.ACTION_USER_PRESENT);

        registerReceiver(screenReceiver, screenFilter);

        IntentFilter appFilter = new IntentFilter();
        appFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appFilter.addDataScheme("package");

        registerReceiver(appReceiver, appFilter);

        IntentFilter dateFilter = new IntentFilter();
        dateFilter.addAction(Intent.ACTION_TIME_TICK);

        registerReceiver(dateReceiver, dateFilter);
    }

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = "";
            switch (intent.getAction()){
                case Intent.ACTION_SCREEN_ON:
                    result = "Screen on";
                    Log.i("from screen service", "Screen on");
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    Log.i("from screen service", "Screen off");
                    result = "Screen off";
                    stopService(new Intent(getApplicationContext(), collectingData.class));
                    break;
                case Intent.ACTION_USER_PRESENT:
                    result = "phone unlocked";
                    Log.i("from screen service", "Phone unlocked");
                    startService(new Intent(getApplicationContext(), collectingData.class));
                    break;
            }
            storeInternally(result);
        }
    };

    private final BroadcastReceiver appReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = "";
            switch (intent.getAction()){
                case Intent.ACTION_PACKAGE_ADDED:
                    Collection<String> entry = getNewApp();
                    result = "App added: " + entry;
                    Log.i("from screen service", "App added: " + entry);
                    updatePrefs();
                    break;
                case Intent.ACTION_PACKAGE_REMOVED:
                    Collection<String> entry1 = getOldApp();
                    result = "App deleted: " + entry1;
                    Log.i("from screen service", "App deleted: " + entry1);
                    updatePrefs();
                    break;
            }
            storeInternally(result);
        }
    };

    private final BroadcastReceiver dateReceiver = new BroadcastReceiver (){
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case Intent.ACTION_TIME_TICK:
                    detectFortnight();
                    break;
            }
        }
    };

    private void detectFortnight() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int initialDay = preferences.getInt("date started", 0);
        int currentDay= cal.get(Calendar.DAY_OF_YEAR);

        Log.i("From screenService", "current hour" + currentDay + " : from prefs: " + initialDay);

        if(currentDay > initialDay){
            startService(new Intent(this, notificationService.class));
        }
    }

    private void updatePrefs() {

        SharedPreferences preferences = getSharedPreferences("Apps", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN,null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List pkgAppList = getPackageManager().queryIntentActivities(mainIntent,0);

        int appNum = 0;
        for (Object object: pkgAppList){
            ResolveInfo info = (ResolveInfo) object;
            appNum++;
            editor.putString("App number " + appNum, ""+ getBaseContext().getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo));
        }
        editor.putInt("Number of apps", appNum);
        editor.apply();
    }

    private Collection<String> getNewApp() {
        SharedPreferences preferences = getSharedPreferences("Apps", Context.MODE_PRIVATE);
        int numOfApps = preferences.getInt("Number of apps", 0);
        Collection<String> appNames = new ArrayList<>();
        for (int i = 0; i < numOfApps; i++) {
            appNames.add(preferences.getString("App number " + (i + 1), "false"));
        }

        Collection<String> newApps = new ArrayList<>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN,null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List pkgAppList = getPackageManager().queryIntentActivities(mainIntent,0);

        for (Object object: pkgAppList){
            ResolveInfo info = (ResolveInfo) object;
            newApps.add(""+getBaseContext().getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo));
        }

        newApps.removeAll(appNames);
       return newApps;
    }

    private void storeInternally(String result) {
        FileOutputStream fileOutputStream = null;
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        String dataEntry = cal.get(Calendar.HOUR_OF_DAY) + "." +
                cal.get(Calendar.MINUTE) + "." +
                cal.get(Calendar.SECOND) +" - " +
                result +":" + "\n";

        try{
            fileOutputStream = openFileOutput(Constants.STATS_FILE_NAME, MODE_APPEND);
            fileOutputStream.write(dataEntry.getBytes());
            Log.i(TAG, dataEntry);
        } catch (IOException storageError){
            Log.e(TAG,"Storing screen state internally issue:  "+ storageError);
        }

        finally{
            try{
                fileOutputStream.close();
            } catch (IOException closureError){
                Log.e(TAG,"Closing screen state internally issue:  "+ closureError);
            }
        }
    }

    private Collection<String> getOldApp() {
        SharedPreferences preferences = getSharedPreferences("Apps", Context.MODE_PRIVATE);
        int numOfApps = preferences.getInt("Number of apps", 0);
        Collection<String> appNames = new ArrayList<>();
        for (int i = 0; i < numOfApps; i++) {
            appNames.add(preferences.getString("App number " + (i + 1), "false"));
        }

        Collection<String> newApps = new ArrayList<>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN,null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List pkgAppList = getPackageManager().queryIntentActivities(mainIntent,0);

        for (Object object: pkgAppList){
            ResolveInfo info = (ResolveInfo) object;
            newApps.add(""+getBaseContext().getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo));
        }
        appNames.removeAll(newApps);
        return appNames;
    }
}