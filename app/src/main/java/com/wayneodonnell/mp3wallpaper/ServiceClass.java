package com.wayneodonnell.mp3wallpaper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.wayneodonnell.mp3wallpaper.models.SongInfo;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ServiceClass extends Service{
    private final static String APP_PACKAGE = "com.wayneodonnell.mp3wallpaper";
    private final static String CHANNEL_ID = APP_PACKAGE + ".NOTIFICATIONS";
    NotificationManager notificationManager;
    NotificationChannel channel;
    String currentPath="";
    String currentAlbum="";
    String currentArtist="";

    ArrayList<String> mFileList=new ArrayList<String>();
    ArrayList<String> mFavouriteList=new ArrayList<String>();
    ArrayList<String> mBlacklist=new ArrayList<String>();

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    Random rnd= new Random();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Get list of files from Shared Preferences

        // set up shared preferences
        mSharedPreferences= PreferenceManager.getDefaultSharedPreferences(ServiceClass.this);
        mEditor=mSharedPreferences.edit();

        String startFolder = mSharedPreferences.getString(Constants.PREFERENCES_STARTFOLDER, null);
        if(!TextUtils.isEmpty(startFolder)) {

            loadSavedLists();
            Random rand=new Random();
            //Pick a random entry from the list, which isn't on the blacklist
            int rnd = rand.nextInt(mFileList.size() - 1);
            //Ensure image not on blacklist and not already in collage
            while(mBlacklist.contains(mFileList.get(rnd))  ){
                rnd = rand.nextInt(mFileList.size() - 1);
            }
            SongInfo song=new SongInfo(mFileList.get(rnd));
            currentPath=mFileList.get(rnd);
            currentArtist=song.getArtistName();
            currentAlbum=song.getAlbumTitle();
            Bitmap bm=song.getAlbumCover();
            setPaper(bm);

            String notification="Wallpaper set to "+currentAlbum+" by "+currentArtist;
            buildNotification(notification,bm);
        }
        return Service.START_NOT_STICKY;  //START_STICKY to restart if stopped
    }

    @Override
    public IBinder onBind(Intent intent) {
        //for communication return IBinder implementation
        return null;
    }

    public void buildNotification(String notificationMsg, Bitmap bm) {

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    CHANNEL_ID,
                    "mp3Wallpaper",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("mp3Wallpaper notifications");
        }

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, MainActivity.class).putExtra("importance",
                            channel.getImportance()).putExtra("channel_id", ""), PendingIntent.FLAG_UPDATE_CURRENT);
        }


        NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle().bigPicture(bm);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(notificationMsg)
                .setGroup("mp3Wallpaper")
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.baseline_music_note_24)
                .setStyle(style);
                //.setSubText(notificationMsg);

        notificationManager.notify(2, notification.build());
    }

    private List<File> getListFiles(File parentDir) {
        //Find only mp3 files
        Boolean alreadyAdded=false;

        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                if(file.getName().endsWith(".mp3")){
                    if(!alreadyAdded) {
                        alreadyAdded=true;
                        inFiles.add(file);
                    }
                }
            }
        }

        return inFiles;
    }
    public void loadSavedLists(){
        //Load saved lists - if main list not found then recreate and save

        //Read album data
        String albumFileString=readFile(Constants.ALBUMS_FILENAME);
        if(!albumFileString.equals("ERROR")){
            //Convert string to JSONArray
            try {
                JSONArray jsonArray = new JSONArray(albumFileString);
                //Populate ArrayList with JSONArray
                for (int i = 0; i < jsonArray.length(); i++) {
                    mFileList.add(jsonArray.getString(i));
                }

                Collections.sort(mFileList); //Ensure list is sorted
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Retrieve favourites
        String favouritesFileString=readFile(Constants.FAVOURITES_FILENAME);
        if(!favouritesFileString.equals("ERROR")) {
            //Convert string to JSONArray
            try {
                JSONArray jsonArray = new JSONArray(favouritesFileString);
                //Populate ArrayList with JSONArray
                for (int i = 0; i < jsonArray.length(); i++) {
                    mFavouriteList.add(jsonArray.getString(i));
                }

                Collections.sort(mFavouriteList); //Ensure list is sorted
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //Retrieve blacklist
        String blacklistFileString=readFile(Constants.BLACKLIST_FILENAME);
        if(!blacklistFileString.equals("ERROR")) {
            //Convert string to JSONArray
            try {
                JSONArray jsonArray = new JSONArray(blacklistFileString);
                //Populate ArrayList with JSONArray
                for (int i = 0; i < jsonArray.length(); i++) {
                    mBlacklist.add(jsonArray.getString(i));
                }

                Collections.sort(mFavouriteList); //Ensure list is sorted
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String readFile(String filename){
        FileInputStream inputStream;
        try {
            FileInputStream in = openFileInput(filename);
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ERROR";
    }

    public void setPaper(Bitmap bm){
        //Set wallpaper to content of imageView
                int params=WallpaperManager.FLAG_LOCK + WallpaperManager.FLAG_SYSTEM;

                    WallpaperManager myWallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                    try {

                        myWallpaperManager.setBitmap(bm, null, false,params);
                        //Store album name and artist to shared preferences
                        addToSharedPreferences(Constants.PREFERENCES_LASTPATH,currentPath); //Get from global variable
                        addToSharedPreferences(Constants.PREFERENCES_LASTALBUM,currentAlbum); //Get from global variable
                        addToSharedPreferences(Constants.PREFERENCES_LASTARTIST,currentArtist); //Get from global variable
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
        //Ask user where to set wallpaper then set Home screen and/or lock screen accordingly.
    }

    public void addToSharedPreferences(String key, String value){
        mEditor.putString(key,value).apply();
    }
}