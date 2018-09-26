package com.wayneodonnell.mp3wallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        startService(context);
    }

    public void startService(Context context) {
        Intent service1 = new Intent(context, ServiceClass.class);
        context.startService(service1);
    }

}