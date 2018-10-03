package com.wayneodonnell.mp3wallpaper;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import java.util.Calendar;

//Receiver called on reboot to reset alarm if necessary

public class BootReceiver extends BroadcastReceiver {

    private SharedPreferences mSharedPreferences;
    private final static String APP_PACKAGE = "com.wayneodonnell.mp3wallpaper";
    private final static String CHANNEL_ID = APP_PACKAGE + ".NOTIFICATIONS";
    NotificationManager notificationManager;
    NotificationChannel channel;

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

            //Show notification advising daily updates turned on
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                channel = new NotificationChannel(
                        CHANNEL_ID,
                        "mp3Wallpaper",
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("mp3Wallpaper notifications");
            }

            PendingIntent contentIntent = PendingIntent.getActivity(context.getApplicationContext(), 0, new Intent(context.getApplicationContext(), MainActivity.class), 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                contentIntent = PendingIntent.getActivity(context.getApplicationContext(), 0,
                        new Intent(context.getApplicationContext(), MainActivity.class).putExtra("importance",
                                channel.getImportance()).putExtra("channel_id", ""), PendingIntent.FLAG_UPDATE_CURRENT);
            }

            NotificationCompat.Builder notification = new NotificationCompat.Builder(context.getApplicationContext(), CHANNEL_ID)
                    .setContentTitle("mp3Wallpaper")
                    //.setContentText(Resources.getSystem().getString(R.string.daily_updates))
                    .setContentText("Daily updates enabled.")
                    .setOngoing(true)
                    .setGroup("mp3Wallpaper")
                    .setContentIntent(contentIntent)
                    .setSmallIcon(R.drawable.baseline_music_note_24);
            //.setSubText(notificationMsg);

            notificationManager.notify(1, notification.build());

        }
    }

}