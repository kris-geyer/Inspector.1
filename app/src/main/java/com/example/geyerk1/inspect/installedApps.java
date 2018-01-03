package com.example.geyerk1.inspect;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

public class installedApps extends Service{

    List pkgAppList;
    StringBuilder builder;

    @Override
    public void onCreate() {
        super.onCreate();
        documentApps();
    }

    private void documentApps() {
        StringBuilder recordedApps = surveyApps();
        storeInternally(recordedApps);
    }

    private StringBuilder surveyApps() {
        SharedPreferences preferences = getSharedPreferences("Apps", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN,null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        pkgAppList = getPackageManager().queryIntentActivities(mainIntent,0);
        builder = new StringBuilder();
        int appNum = 0;
            for (Object object: pkgAppList){
                ResolveInfo info = (ResolveInfo) object;
                builder.append(getBaseContext().getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo) + "\n");
                appNum++;
                editor.putString("App number " + appNum, ""+ getBaseContext().getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo));
            }
            editor.putInt("Number of apps", appNum);
            editor.apply();
        return builder;
    }

    private void storeInternally(StringBuilder result) {
        FileOutputStream fileOutputStream = null;
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        String dataEntry = cal.get(Calendar.HOUR_OF_DAY) + "." +
                cal.get(Calendar.MINUTE) + "." +
                cal.get(Calendar.SECOND) +" - " +
                result +":" + "\n";

        try{
            fileOutputStream = openFileOutput(Constants.APP_LIST_FILE_NAME, MODE_APPEND);
            fileOutputStream.write(dataEntry.getBytes());
            Log.i(TAG, dataEntry);
        } catch (IOException storageError){
            Log.e(TAG,"Storing screen state internally issue:  "+ storageError);
        }

        finally{
            try{
                fileOutputStream.close();
                stopSelf();
            } catch (IOException closureError){
                Log.e(TAG,"Closing screen state internally issue:  "+ closureError);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
