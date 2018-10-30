package com.wayneodonnell.mp3wallpaper;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {
    private final static String APP_PACKAGE = "com.wayneodonnell.mp3wallpaper";
    private final static String CHANNEL_ID = APP_PACKAGE + ".NOTIFICATIONS";
    NotificationManager notificationManager;
    NotificationChannel channel;

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

        //Intent alarmIntent = new Intent(context.getApplicationContext(), AlarmReceiver.class);

        Intent alarmIntent = new Intent("com.wayneodonnell.mp3wallpaper.CUSTOM_ALARM");
        PendingIntent pi = PendingIntent.getBroadcast(context.getApplicationContext(), 0, alarmIntent, 0);

        //Start at midnight and repeat every 24 hours
        alarm_manager.cancel(pi);
        pi.cancel();
        alarm_manager.setAndAllowWhileIdle(AlarmManager.RTC, cur_cal.getTimeInMillis(), pi); //Set the alarm

        //Show notification advising daily updates turned on
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //Clear the existing notification
        notificationManager.cancel(1);

        //Build new notification
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
                            channel.getImportance()).putExtra("channel_id", CHANNEL_ID), PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context.getApplicationContext(), CHANNEL_ID)
                .setContentTitle("mp3Wallpaper")
                .setContentText("Daily updates enabled.")
                .setOngoing(true)
                .setGroup("mp3Wallpaper")
                .setContentIntent(contentIntent)
                .setChannelId(CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_music_note_24);
        //.setSubText(notificationMsg);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(1, notification.build());


        startService(context);
    }

    public void startService(Context context) {
        Intent service1 = new Intent(context, ServiceClass.class);
        context.startService(service1);
    }

}