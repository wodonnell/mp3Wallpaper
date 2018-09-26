package com.wayneodonnell.mp3wallpaper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.Calendar;

//Receiver called on reboot to reset alarm if necessary

public class BootReceiver  extends BroadcastReceiver {

    private SharedPreferences mSharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        setAlarm(context);
    }

    public void setAlarm(Context context){
        mSharedPreferences= PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Boolean timerActive = mSharedPreferences.getBoolean(Constants.PREFERENCES_TIMERACTIVE, false);

        if(timerActive) {
            //Set the alarm
            Calendar cur_cal = Calendar.getInstance();
            cur_cal.setTimeInMillis(System.currentTimeMillis());
            cur_cal.set(Calendar.HOUR_OF_DAY, 23); // Set to 23:59 today then add one minute for midnight
            cur_cal.set(Calendar.MINUTE, 59);
            cur_cal.set(Calendar.SECOND, 00);
            cur_cal.add(Calendar.SECOND, 60);
            //cur_cal.add(Calendar.SECOND, 1); //Start straight away

            AlarmManager alarm_manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent intent = new Intent(context.getApplicationContext(), AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context.getApplicationContext(), 0, intent, 0);

            //Start at midnight and repeat every 24 hours
            alarm_manager.setInexactRepeating(AlarmManager.RTC, cur_cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi); //Repeat every day
            //alarm_manager.setInexactRepeating(AlarmManager.RTC, cur_cal.getTimeInMillis(),AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi); //Repeat every 15 minutes
        }
    }

}