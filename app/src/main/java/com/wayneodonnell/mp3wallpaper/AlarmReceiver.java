package com.wayneodonnell.mp3wallpaper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //Reset the alarm

        //Set the alarm
        Calendar cur_cal = Calendar.getInstance();
        cur_cal.setTimeInMillis(System.currentTimeMillis());
        cur_cal.set(Calendar.HOUR_OF_DAY, 23); // Set to 23:59 today then add one minute for midnight
        cur_cal.set(Calendar.MINUTE, 59);
        cur_cal.set(Calendar.SECOND, 00);
        cur_cal.add(Calendar.SECOND, 60);
        //cur_cal.add(Calendar.SECOND, 30); //Start in 30 seconds

        AlarmManager alarm_manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent alarmIntent = new Intent(context.getApplicationContext(), AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context.getApplicationContext(), 0, alarmIntent, 0);

        //Start at midnight and repeat every 24 hours
        alarm_manager.cancel(pi);
        alarm_manager.setAndAllowWhileIdle(AlarmManager.RTC, cur_cal.getTimeInMillis(), pi); //Set the alarm

        startService(context);
    }

    public void startService(Context context) {
        Intent service1 = new Intent(context, ServiceClass.class);
        context.startService(service1);
    }

}