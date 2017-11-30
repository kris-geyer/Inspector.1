package com.example.geyerk1.inspect;

import android.annotation.SuppressLint;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static android.content.ContentValues.TAG;

public class collectingData extends Service {

    Handler statsHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"Service started");
        statsHandler = new Handler();
        statsHandler.postDelayed(runnable, 500);
        return START_NOT_STICKY;
    }


    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            printForegroundTask();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        statsHandler.removeCallbacks(runnable);
        Log.i(TAG, "Service destroyed");
    }

    private void printForegroundTask() {
        String currentApp = "NULL";

        @SuppressLint("WrongConstant") UsageStatsManager usm = (UsageStatsManager)this.getSystemService("usagestats");
        long time = System.currentTimeMillis();
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  time - 1000*1000, time);
        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (mySortedMap != null && !mySortedMap.isEmpty()) {
                currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                PackageManager packageManager = getApplicationContext().getPackageManager();
                try {
                    currentApp = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(currentApp, PackageManager.GET_META_DATA));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }

        }
        statsHandler.postDelayed(runnable, 10000);

        storeStats(currentApp);

    }

    private void storeStats(String currentApp) {

        FileOutputStream fileOutputStream = null;
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        String dataEntry = "Time: " +
                cal.get(Calendar.HOUR_OF_DAY) + "." +
                cal.get(Calendar.MINUTE) + "." +
                cal.get(Calendar.SECOND) +" - App: " +
                currentApp + ";" + "\n";

        try{
            fileOutputStream = openFileOutput(Constants.STATS_FILE_NAME, MODE_APPEND);
            fileOutputStream.write(dataEntry.getBytes());
            Log.i(TAG, dataEntry);
        } catch (IOException storageError){
           Log.e(TAG,"Storing stats internally issue:  "+ storageError);
        }

        finally{
            try{
                fileOutputStream.close();
            } catch (IOException closureError){
                Log.e(TAG,"Closing stats internally issue:  "+ closureError);
            }
        }

    }
}
