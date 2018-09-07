package com.wayneodonnell.mp3wallpaper;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    @BindView(R.id.btnSetWallpaper)  Button mBtnSetWallpaper;
    @BindView(R.id.btnRandom)  Button mBtnRandom;
    @BindView(R.id.imageView) ImageView mImageView;

    ArrayList<String> fileList=new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        /* Set onClick listeners */
        mBtnSetWallpaper.setOnClickListener(this);
        mBtnRandom.setOnClickListener(this);
        //Get list of files when app started
        getFileList();
    }

    @Override
    public void onClick(View v){
        if(v==mBtnSetWallpaper){
            /* Clear the image */
            //mImageView.setImageResource(0);
            setPaper(mImageView);
            Toast.makeText(MainActivity.this,"Wallpaper set",Toast.LENGTH_LONG).show();
        }
        if(v==mBtnRandom){
            String filepath=getFilePath();

            if(filepath!="") {
                //Get album art from mp3 file
                Bitmap bm=extractAlbumArt(filepath);
                //Set ImageView to use bitmap
                if(bm!=null){
                    mImageView.setImageBitmap(bm);
                }
            }

        }
    }
    public void getFileList() {
        //Before accessing files, need to check permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            //Need to request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }

        List<File> files = getListFiles(new File("/storage/C219-D78B/Music/"));

        //Add each file name to the list
        for(File file: files){
            fileList.add(file.toString());
        }

    }

    public String getFilePath(){
        //String sdpath = Environment.getExternalStorageDirectory().getAbsolutePath();
        //String sdpath= System.getenv("EXTERNAL_STORAGE");
        //String sdpath="/storage/C219-D78B";  //Hardcoded path - need to work out how to find this!
        //String sdpath="/storage/emulated/0/Download";  //Hardcoded path - need to work out how to find this!
        String filepath="";

        //Create a list of some files to read - eventually this will read through the SD card


        /*fileList.add("/storage/C219-D78B/Music/Bon Jovi/Keep The Faith/03 I'll Sleep When I'm Dead.mp3");
        fileList.add("/storage/C219-D78B/Music//Cast/All Change/03 Sandstorm.mp3");
        fileList.add("/storage/C219-D78B/Music//The Police/The Very Best Of Sting And The Police/03 Englishman In New York.mp3");
        fileList.add("/storage/C219-D78B/Music//Slade/Sladest/01 Cum On Feel The Noize.mp3");
        fileList.add("/storage/C219-D78B/Music//Queen/Hot Space/09 Las Palabras De Amor (The Words O.mp3");
*/
        //Pick one of the files at random
        if(fileList.size()>0){
            Random rand = new Random();
            int rnd = rand.nextInt(fileList.size() - 1);

            //Pick a random file
            filepath = fileList.get(rnd);
        }
        return filepath;
    }

    public void setPaper(ImageView imageView){
        //Set wallpaper to content of imageView
        WallpaperManager myWallpaperManager
                = WallpaperManager.getInstance(getApplicationContext());
        try {
            BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
            myWallpaperManager.setBitmap(drawable.getBitmap());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap extractAlbumArt(String filename){
        //Extract the embedded image from the specified file
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(filename);

        byte [] data = mmr.getEmbeddedPicture();

        Bitmap bm=null;

        // convert the byte array to a bitmap
        if(data != null) {
             bm = BitmapFactory.decodeByteArray(data, 0, data.length);
        }

        return bm;  //Return the bitmap
    }

    private List<File> getListFiles(File parentDir) {
        List<File> inFiles = new ArrayList<>();
        Queue<File> files = new LinkedList<>();
        files.addAll(Arrays.asList(parentDir.listFiles()));
        while (!files.isEmpty()) {
            File file = files.remove();
            if (file.isDirectory()) {
                files.addAll(Arrays.asList(file.listFiles()));
            } else if (file.getName().endsWith(".mp3")) {
                inFiles.add(file);
            }
        }
        return inFiles;
    }
}
