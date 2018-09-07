package com.wayneodonnell.mp3wallpaper;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicResize;
import android.renderscript.Type;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.renderscript.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
    @BindView(R.id.btnCollage)  Button mBtnCollage;

    ArrayList<String> mFileList=new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        /* Set onClick listeners */
        mBtnSetWallpaper.setOnClickListener(this);
        mBtnRandom.setOnClickListener(this);
        mBtnCollage.setOnClickListener(this);
        //Get list of files when app started
        getFileList();
    }

    @Override
    public void onClick(View v){
        //TODO - Create a random collage
        if(v==mBtnSetWallpaper){
            /* Clear the image */
            //mImageView.setImageResource(0);
            setPaper(mImageView);
            Toast.makeText(MainActivity.this,"Wallpaper set",Toast.LENGTH_LONG).show();
        }
        if(v==mBtnCollage)
        {
            mImageView.setImageBitmap(createCollage());
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
            // If permission is not granted need to request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }

        List<File> files = getListFiles(new File("/storage/C219-D78B/Music/"));

        //Add each file, add name to the list
        for(File file: files){
            mFileList.add(file.toString());
        }
    }

    public String getFilePath(){
        //TODO - Find actual path to SDCard
        //String sdpath = Environment.getExternalStorageDirectory().getAbsolutePath();
        //String sdpath= System.getenv("EXTERNAL_STORAGE");
        //String sdpath="/storage/C219-D78B";  //Hardcoded path - need to work out how to find this!
        //String sdpath="/storage/emulated/0/Download";  //Hardcoded path - need to work out how to find this!
        String filepath="";
        //Pick one of the files at random
        if(mFileList.size()>1){
            Random rand = new Random();
            int rnd = rand.nextInt(mFileList.size() - 1);

            //Pick a random file
            filepath = mFileList.get(rnd);
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
        //Find only mp3 files
        Boolean alreadyAdded=false;
  /*      List<File> inFiles = new ArrayList<>();
        Queue<File> files = new LinkedList<>();
        files.addAll(Arrays.asList(parentDir.listFiles()));
        while (!files.isEmpty()) {
            File file = files.remove();
            if (file.isDirectory()) {
                files.addAll(Arrays.asList(file.listFiles()));
            } else if (file.getName().endsWith(".mp3")) {
                //Only add if a file from this folder has not already been added
                if(!alreadyAdded) {
                    alreadyAdded=true;
                    inFiles.add(file);
                }
            }
        }

*/
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

    private Bitmap createCollage(){
        // TODO - Creates a collage of 16 bitmaps
        // TODO - Collage needs to resize images so all same size
        //Get bitmaps
        String[] collagelist=new String[16];

        //Shuffle the collection
        Collections.shuffle(mFileList,new Random());

        Bitmap[] collagebitmaps=new Bitmap[16];

        //Because mFileList is shuffled, we can just take the first 16
        for(int i=0;i<16;i++){
            //ScaledBitmap not very good quality
            //collagebitmaps[i]= Bitmap.createScaledBitmap(extractAlbumArt(collagelist[i]),100,100,false);
            RenderScript RS = RenderScript.create(this);
            collagebitmaps[i]= resizeBitmap(RS,extractAlbumArt(mFileList.get(i)),100);
        }

        Bitmap result = Bitmap.createBitmap(collagebitmaps[0].getWidth() * 4, collagebitmaps[0].getHeight() * 4, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        for (int i = 0; i < collagebitmaps.length; i++) {
            canvas.drawBitmap(collagebitmaps[i], collagebitmaps[i].getWidth() * (i % 4), collagebitmaps[i].getHeight() * (i / 4), paint);
        }
        return result;
    }
    public static Bitmap resizeBitmap(RenderScript rs, Bitmap src, int dstWidth) {
        Bitmap.Config  bitmapConfig = src.getConfig();
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        float srcAspectRatio = (float) srcWidth / srcHeight;
        int dstHeight = (int) (dstWidth / srcAspectRatio);

        float resizeRatio = (float) srcWidth / dstWidth;

        /* Calculate gaussian's radius */
        float sigma = resizeRatio / (float) Math.PI;
        // https://android.googlesource.com/platform/frameworks/rs/+/master/cpu_ref/rsCpuIntrinsicBlur.cpp
        float radius = 2.5f * sigma - 1.5f;
        radius = Math.min(25, Math.max(0.0001f, radius));

        /* Gaussian filter */
        Allocation tmpIn = Allocation.createFromBitmap(rs, src);
        Allocation tmpFiltered = Allocation.createTyped(rs, tmpIn.getType());
        ScriptIntrinsicBlur blurInstrinsic = ScriptIntrinsicBlur.create(rs, tmpIn.getElement());

        blurInstrinsic.setRadius(radius);
        blurInstrinsic.setInput(tmpIn);
        blurInstrinsic.forEach(tmpFiltered);

        tmpIn.destroy();
        blurInstrinsic.destroy();

        /* Resize */
        Bitmap dst = Bitmap.createBitmap(dstWidth, dstHeight, bitmapConfig);
        Type t = Type.createXY(rs, tmpFiltered.getElement(), dstWidth, dstHeight);
        Allocation tmpOut = Allocation.createTyped(rs, t);
        ScriptIntrinsicResize resizeIntrinsic = ScriptIntrinsicResize.create(rs);

        resizeIntrinsic.setInput(tmpFiltered);
        resizeIntrinsic.forEach_bicubic(tmpOut);
        tmpOut.copyTo(dst);

        tmpFiltered.destroy();
        tmpOut.destroy();
        resizeIntrinsic.destroy();

        return dst;
    }
}
