package com.example.geyerk1.inspect;


import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static android.content.ContentValues.TAG;

public class phonerestarted extends Service{
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if( preferences.getBoolean("collect data", true)){
            Log.i("From mainActivity", "going to start Service");
            startService(new Intent(getApplicationContext(), screenService.class));
        documentRestart();
        }
        return START_NOT_STICKY;
    }

    private void documentRestart() {

        FileOutputStream fileOutputStream = null;
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        String dataEntry = cal.get(Calendar.HOUR_OF_DAY) + "." +
                cal.get(Calendar.MINUTE) + "." +
                cal.get(Calendar.SECOND) +" - " +
                "Phone restarted" +":" + "\n";

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
        stopSelf();
    }
}
