package com.wayneodonnell.mp3wallpaper;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    @BindView(R.id.btnSetWallpaper)  Button mBtnSetWallpaper;
    @BindView(R.id.btnRandom)  Button mBtnRandom;
    @BindView(R.id.imageView) ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        /* Set onClick listeners */
        mBtnSetWallpaper.setOnClickListener(this);
        mBtnRandom.setOnClickListener(this);
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
                /*
                File imageFile = new File(filepath); //Define actual file
                FileInputStream fis = null;  //Create a filestream
                // Check file exists and if so, populate filestream
                try {
                    fis = new FileInputStream(imageFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                //Create a bitmap using the filestream
                Bitmap bm = BitmapFactory.decodeStream(fis);
*/
                Bitmap bm=extractAlbumArt(filepath);
                //Set ImageView to use bitmap
                if(bm!=null){
                    mImageView.setImageBitmap(bm);
                }
            }

        }
    }
    public String getFilePath(){
        //String sdpath = Environment.getExternalStorageDirectory().getAbsolutePath();
        //String sdpath= System.getenv("EXTERNAL_STORAGE");
        //String sdpath="/storage/C219-D78B";  //Hardcoded path - need to work out how to find this!
        //String sdpath="/storage/emulated/0/Download";  //Hardcoded path - need to work out how to find this!
        String filepath="";

        //Before accessing files, need to check permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            //Need to request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
        //Pick one of the 2 files at random
        //Random rand=new Random();
        //int rnd = rand.nextInt(10) + 1;
        //if(rnd<=5){
//            filepath="/storage/emulated/0";

        //}
        //else{
            filepath="/storage/C219-D78B/Music/Bon Jovi/Keep The Faith/03 I'll Sleep When I'm Dead.mp3";
  //      }
        return filepath;
    }

    public void setPaper(ImageView imageview){

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
}
