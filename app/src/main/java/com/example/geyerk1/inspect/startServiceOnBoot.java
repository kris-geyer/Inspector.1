package com.example.geyerk1.inspect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class startServiceOnBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            Intent initiateServices = new Intent(context, phonerestarted.class);
            context.startService(initiateServices);
        }
    }
}
