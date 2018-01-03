package com.example.geyerk1.inspect;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import android.preference.PreferenceManager;
import android.provider.Settings;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends Activity implements View.OnClickListener{

    Button email, deleteFunction;
    TextView result;

    private static final int MY_PERMISSION_REQUEST_EXTERNAL_STORAGE = 100;
    private static final int MY_PERMISSION_REQUEST_PACKAGE_USAGE_STATS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firstActivated();
        initiate();
        detectStateOfPackagePermissions();
        uninstallApp();
    }

    private void firstActivated() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if(preferences.getBoolean("first log on", true)){
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("first log on", false);
            editor.apply();

            Date date = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            //for testing this is changed to hour
            editor.putInt("date started",cal.get(Calendar.DAY_OF_YEAR));
            editor.apply();
            Log.i("from main", "data started: " + preferences.getInt("date started", 0));
        }
    }

    private void uninstallApp() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if(preferences.getBoolean("start uninstall", false)){

            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:com.example.geyerk1.inspect"));
            startActivity(intent);
        }
    }

    private void initiate() {
        result = findViewById(R.id.tvResult);

        email = findViewById(R.id.btnEmail);
        email.setOnClickListener(this);
        deleteFunction = findViewById(R.id.btnDelete);
        deleteFunction.setOnClickListener(this);
    }

    private void detectStateOfPackagePermissions() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if(hasPermission() && preferences.getBoolean("collect data", true)){
            Log.i("From mainActivity", "going to start Service");
            startService(new Intent(this, screenService.class));
        }
        else if(hasPermission() && !preferences.getBoolean("collect data", true)){
            Log.i("From mainActivity", "going to start Notification");
            startService(new Intent(this, notificationService.class));
        }
        else{
            requestPackagePermission(null);
        }
    }

    private void requestPackagePermission(View view) {
        AlertDialog.Builder builder;

        builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Usage permission")
                .setMessage("To participate in this experiment you must enable the permission")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), MY_PERMISSION_REQUEST_PACKAGE_USAGE_STATS);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private boolean hasPermission() {
        AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());

        Log.i("From mainActivity", "Permission given: " + String.valueOf(mode == AppOpsManager.MODE_ALLOWED));
        return mode == AppOpsManager.MODE_ALLOWED;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_PACKAGE_USAGE_STATS:
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                if(hasPermission() &&  preferences.getBoolean("collect data", true)){
                    Log.i("From mainActivity", "going to start Service");
                    startService(new Intent(this, screenService.class));
                }
                else if(hasPermission() && !preferences.getBoolean("collect data", true)){
                    Log.i("From mainActivity", "going to Notification");
                    startService(new Intent(this, notificationService.class));
                }
                else{
                    requestPackagePermission(null);
                }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.btnEmail:
                if (externalStoragePermission()) {
                    emailData(null);
                } else {
                    requestExternalStoragePermission();
                }
                break;
            case R.id.btnDelete:
                deleteFiles(null);
                break;
        }
    }


    private boolean externalStoragePermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case MY_PERMISSION_REQUEST_EXTERNAL_STORAGE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                    emailData(null);
                }
                else{
                    Toast.makeText(this, "This permission is required for participating in the study", Toast.LENGTH_SHORT).show();
                }
        }
    }
    public String read_file(Context context, String filename) {
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception error) {
            return "";
        }
    }

    public void emailData(View v) {

        String statsRaw = read_file(getApplicationContext(), Constants.STATS_FILE_NAME);

        statsRaw = encrypter(statsRaw);

        File statsFile;
        FileOutputStream statsOutputStream;

        String appRaw = read_file(getApplicationContext(), Constants.APP_LIST_FILE_NAME);

        File appFile;
        FileOutputStream appOutputStream;
        try {
            statsFile = new File(Environment.getExternalStorageDirectory(), "statsRecords");

            statsOutputStream = new FileOutputStream(statsFile);
            statsOutputStream.write(statsRaw.getBytes());
            statsOutputStream.close();

            appFile = new File(Environment.getExternalStorageDirectory(), "appsRecords");

            appOutputStream = new FileOutputStream(appFile);
            appOutputStream.write(appRaw.getBytes());
            appOutputStream.close();

            String[] TO = {"geyerkristoffer@gmail.com"};

            Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);

            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.setType("application/YourMimeType");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "My Activity data");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "I have attached the data relating to my phone activity.");

            ArrayList<Uri> uris = new ArrayList<Uri>();
            File statsUFile = new File(String.valueOf(statsFile));
            Uri uStats = Uri.fromFile(statsUFile);
            uris.add(uStats);

            File appsUFile = new File(String.valueOf(appFile));
            Uri uApps = Uri.fromFile(appsUFile);
            uris.add(uApps);

            emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            getApplicationContext().startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String encrypter(String toChange) {
        ArrayList<Character> ASI11Holder = new ArrayList<>();
        int position = 0;
        int toChangeBy = 0;
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        month += 1;

        Log.i("from main", "Date " + day + "," + month + "," + year);

        for (int i = 0; i < toChange.length();i++ ){

            //establishing the degree tht the character should be changed by
            switch(position){
                //first digit
                case 0:
                    //handles what should happen if there is only one digit in the month
                    if (day<10){
                        toChangeBy = 1;
                    }
                    else{
                        toChangeBy = Integer.parseInt(Integer.toString(day).substring(0,1));
                    }
                    break;
                //second digit
                case 1:
                    toChangeBy = Integer.parseInt(Integer.toString(day).substring(1,2));
                    break;
                //third digit
                case 2:
                    toChangeBy = Integer.parseInt(Integer.toString(month).substring(0,1));
                    break;
                case 3:
                    toChangeBy = Integer.parseInt(Integer.toString(month).substring(1,2));
                    break;
                case 4:
                    toChangeBy = Integer.parseInt(Integer.toString(year).substring(0,1));
                    break;
                case 5:
                    toChangeBy = Integer.parseInt(Integer.toString(year).substring(1,2));
                    break;
                case 6:
                    toChangeBy = Integer.parseInt(Integer.toString(year).substring(2,3));
                    break;
                case 7:
                    toChangeBy = Integer.parseInt(Integer.toString(year).substring(3,4));
                    break;
            }
            if(position < 7){
                position++;
            }
            else {
                position = 0;
            }
            char c = toChange.charAt(i);
            char readableC = c;
            if(Character.isLetter(c)) {
                if(Character.isUpperCase(c)) {
                    c =   Character.toLowerCase(c);
                }
                int newC = (int) c;
                newC += toChangeBy;
                if (newC > 122) {
                    newC -= 26;
                }
                readableC = (char) newC;
                Log.i("from main", "original char: " + c + ", to change by: " + toChangeBy + ", value after conversion was: " + newC + ", returns: " + readableC);
            }
            ASI11Holder.add(readableC);
        }

        String toReturn = "";
        for (int i = 0; i < ASI11Holder.size(); i++){
            toReturn += String.valueOf(ASI11Holder.get(i)) ;
        }
        return toReturn;
    }

    public void deleteFiles(View view) {

        Context context = MainActivity.this;

        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.search_prompt, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = promptsView.findViewById(R.id.user_input);


        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setNegativeButton("Go",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // DO THE METHOD HERE WHEN PROCEED IS CLICKED*/
                                String user_text = (userInput.getText()).toString();

                                // CHECK FOR USER'S INPUT **/
                                if (user_text.equals("oeg")) {
                                    File file = new File(getFilesDir(), Constants.STATS_FILE_NAME);
                                    if (file.exists()) {
                                        deleteFile(Constants.STATS_FILE_NAME);
                                        File Exfile = new File (Environment.getExternalStorageDirectory(),"ActivationRecords");
                                        Exfile.delete();
                                        Toast.makeText(getApplicationContext(), "File deleted.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "File not found", Toast.LENGTH_SHORT).show();
                                    }

                                } else {
                                    Toast.makeText(getApplicationContext(), "Password incorrect", Toast.LENGTH_SHORT).show();
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                                    builder.setTitle("Error");
                                    builder.setPositiveButton("Cancel", null);
                                    builder.setNegativeButton("Retry", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    });
                                    builder.create().show();

                                }
                            }
                        })
                .setPositiveButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }

                        }

                );

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }


}
