package com.wayneodonnell.mp3wallpaper;

import android.Manifest;
import android.app.FragmentManager;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.preference.PreferenceManager;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicResize;
import android.renderscript.Type;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.support.v7.widget.SearchView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.app.AlertDialog;

import com.wayneodonnell.mp3wallpaper.models.SongInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM;
import static android.media.MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    @BindView(R.id.btnSetWallpaper)  Button mBtnSetWallpaper;
    @BindView(R.id.btnPrev)  Button mBtnPrev;
    @BindView(R.id.btnNext)  Button mBtnNext;
    @BindView(R.id.imageView) ImageView mImageView;
    @BindView(R.id.imageViewBackground) ImageView mImageViewBackground;
    @BindView(R.id.btnCollage)  Button mBtnCollage;

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    ArrayList<String> mFileList=new ArrayList<String>();
    ArrayList<String> mAlbumList=new ArrayList<String>();
    ArrayList<String> mBlacklist=new ArrayList<String>(); // TODO - add blacklist
    ArrayList<String> mFilterList=new ArrayList<String>();
    ArrayList<String> mFavouriteList=new ArrayList<String>(); //TODO - add favourites

    int position=-99; //Start as -99 as this will never be reached again in code
    int favPosition=0;
    int heldPosition=-99;
    int next=1;
    int prev=-1;
    boolean onlyFavourites=false;
    String currentPath="";
    String currentAlbum="";
    String currentArtist="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // set up shared preferences
        mSharedPreferences= PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        mEditor=mSharedPreferences.edit();

        /* Set onClick listeners */
        mBtnSetWallpaper.setOnClickListener(this);
        mBtnPrev.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);
        mBtnCollage.setOnClickListener(this);
        getCurrentPaper(); //Populate screen with current system wallpaper
        //TODO - add button/menu option to refresh list if already loaded.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_filter, menu);
        ButterKnife.bind(this);

        MenuItem menuItem = menu.findItem(R.id.action_filter);
        SearchView searchView=(SearchView) menuItem.getActionView();
        //Add listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                getFileList();  //Ensure list is populated
                heldPosition=position; //Store current position
                getFilterList(query);

                if(mFilterList.size()==0){
                    Toast.makeText(MainActivity.this,"Nothing found for '"+query+"'",Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {

            @Override
            public boolean onClose() {
                mFilterList.clear(); //Clear the filter
                if(heldPosition>-99)
                {
                    position = heldPosition + 1;
                    heldPosition=-99;
                }
                else {
                    position+=1;
                }

                setImage(prev); //Restore previous image
                return false;
            }


        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v){
        //TODO - Option to thumbs down a cover so it doesn't show.
        if(v==mBtnSetWallpaper){
            setPaper(mImageView);
        }
        if(v==mBtnCollage)
        {
            getFileList();
            mImageView.setImageBitmap(createCollage());
            mImageViewBackground.setImageDrawable(mImageView.getDrawable());
        }

        if(v==mBtnNext){
            getFileList();
            setImage(next);
        }
        if(v==mBtnPrev){
            getFileList();
            setImage(prev);
        }
    }
    public void getFilterList(String query){
        //Add each file, add name to the list
        query=query.toLowerCase();
        mFilterList.clear();
        String albumPath="";

        for(String item: mFileList){
            //Check each entry in filelist and add to filter if specified text appears.
            //item=item.toLowerCase();

            albumPath=item.toLowerCase().substring(0,item.toLowerCase().lastIndexOf("/"));
            if(albumPath.contains(query)) {
                mFilterList.add(item);
            }
        }
        if(mFilterList.size()>0){
            //Refresh the list being used
            position = -1;
            setImage(next);
        }
    }

    public void getFileList() {
        ////TODO - Add loading spinner - currently struggling to get this to show
        if(mFileList.size()==0){
            //Before accessing files, need to check permission granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // If permission is not granted need to request permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
            //TODO - Find actual path to SDCard
            //String sdpath = Environment.getExternalStorageDirectory().getAbsolutePath();
            //String sdpath= System.getenv("EXTERNAL_STORAGE");
            //String sdpath="/storage/C219-D78B";  //Hardcoded path - need to work out how to find this!
            //String sdpath="/storage/emulated/0/Download";  //Hardcoded path - need to work out how to find this!

            List<File> files = getListFiles(new File("/storage/C219-D78B/Music/"));

            //Add each file, add name to the list
            String albumPath = "";
            String songPath = "";
            mAlbumList.clear();
            mFileList.clear();
            mFilterList.clear();
            for (File file : files) {
                //Only add if the album hasn't already been added and album isn't on blacklist
                songPath = file.toString();
                albumPath = songPath.substring(0, songPath.lastIndexOf("/"));
                if (!mAlbumList.contains(albumPath) && !mBlacklist.contains(albumPath)) {
                    mAlbumList.add(albumPath);
                    mFileList.add(file.toString());
                }
            }
            //TODO - add option to shuffle the list
            //Collections.shuffle(mFileList,new Random());
            //Sort the list into order
            Collections.sort(mFileList);
        }
    }

    public void setImage(int direction){
        //Pick the next image

        //If a filter is in place then use the filtered list
        //Filter could have been applied to main list or favourites

        if (mFilterList.size() > 0){
            //Get next from mFilterList
            position=setFile(mFilterList,position,direction);
        }
        //If only looking at favourites
        else if(onlyFavourites){
            //Get next from mFavouriteList
            favPosition=setFile(mFavouriteList,favPosition,direction);
        }
        //Otherwise use the main file list
        else if(mFileList.size()>1){
            //Get next from mFileList
            position=setFile(mFileList,position,direction);
        }
    }

    public int setFile(ArrayList<String> aList,int pos, int direction){
        String filepath="";
        //If position is -99 then we have clicked Prev or next for the first time
        //See if current wallpaper is still available in the array and if so, set its position
        if(pos==-99){
            if(aList.contains(currentPath)){
                pos=aList.indexOf(currentPath);
            }
            else {
                pos=0;
            }
        }
        //Get next position
        pos+=direction; // Get next position
        if(pos==aList.size() && direction==1) {
            pos=0;
        }
        if(pos==-1 && direction==-1) {
            pos=aList.size()-1;
        }

        filepath = aList.get(pos);

        if(filepath!="") {
            SongInfo song=new SongInfo(filepath);
            //Get album art from mp3 file
            Bitmap bm=song.getAlbumCover();
            currentPath=filepath; // Set global variable so we always have a record of current path
            currentAlbum=song.getAlbumTitle();
            currentArtist=song.getArtistName();
            //Bitmap bm=extractAlbumArt(filepath);
            //String albumTitle=extractAlbumName(filepath);
            //String artistName=extractArtistName(filepath);
            //Set ImageView to use bitmap
            if(bm!=null){
                //Set the images
                mImageView.setImageBitmap(bm);
                mImageViewBackground.setImageBitmap(bm);
                //TODO - Display album and artist somewhere
                updateMeta(currentAlbum,currentArtist);// Show album and artist
            }
        }

        return pos; //Return updated position
    }

    public void getCurrentPaper(){
        //TODO - getCurrentPaper - retrieve album name and artist from shared preferences
        currentPath=mSharedPreferences.getString(Constants.PREFERENCES_LASTPATH,null);
        String albumName=mSharedPreferences.getString(Constants.PREFERENCES_LASTALBUM,null);
        String artistName=mSharedPreferences.getString(Constants.PREFERENCES_LASTARTIST,null);
        WallpaperManager myWallpaperManager
                = WallpaperManager.getInstance(getApplicationContext());
        Drawable db =  myWallpaperManager.getDrawable();
        if(db!=null){
            mImageView.setImageDrawable(db);
            mImageViewBackground.setImageDrawable(db);
            updateMeta(albumName,artistName);
        }
    }

    public void setPaper(ImageView imageView){
        //Set wallpaper to content of imageView
        //Ask user where to set wallpaper then set Home screen and/or lock screen accordingly.
        String[] options = {"Home screen", "Lock screen", "Home and Lock screens"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Where to set wallpaper?");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // the user clicked on colors[which]
                int params=-1;
                if(which==0){
                    params=WallpaperManager.FLAG_SYSTEM;
                }
                else if(which==1){
                    params=WallpaperManager.FLAG_LOCK;
                }
                else if (which==2) {
                    params=WallpaperManager.FLAG_LOCK + WallpaperManager.FLAG_SYSTEM;
                }
                if(params>-1) {
                    WallpaperManager myWallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                    try {
                        BitmapDrawable drawable = (BitmapDrawable) mImageView.getDrawable();
                        myWallpaperManager.setBitmap(drawable.getBitmap(), null, false,params);
                        Toast.makeText(MainActivity.this, "Wallpaper set", Toast.LENGTH_LONG).show();
                        //TODO - setPaper - store album name and artist to shared preferences
                        addToSharedPreferences(Constants.PREFERENCES_LASTPATH,currentPath); //Get from global variable
                        addToSharedPreferences(Constants.PREFERENCES_LASTALBUM,currentAlbum); //Get from global variable
                        addToSharedPreferences(Constants.PREFERENCES_LASTARTIST,currentArtist); //Get from global variable
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                dialog.cancel();
            }
        });
        builder.show();
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
        //TODO - createCollage - sometimes collage is offset incorrectly
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

            //Get album art from mp3 file
            SongInfo song=new SongInfo(mFileList.get(i));
            Bitmap bm=song.getAlbumCover();
            collagebitmaps[i]= resizeBitmap(RS,bm,100);
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
        resizeRatio=1; //We always want squares in this app.

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

    public void addToSharedPreferences(String key, String value){
        mEditor.putString(key,value).apply();
    }
    public void updateMeta(String album, String artist) {
        //If album name not blank then show details
        if(!album.equals("")) {
            Toast.makeText(MainActivity.this, "Artist: " + artist + "\nAlbum: " + album, Toast.LENGTH_SHORT).show();
        }
    }
}