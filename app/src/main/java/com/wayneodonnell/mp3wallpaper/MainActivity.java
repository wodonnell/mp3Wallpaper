package com.wayneodonnell.mp3wallpaper;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.WallpaperManager;
import android.content.Context;
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
import android.icu.text.UnicodeSetSpanner;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.support.v7.widget.SearchView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import com.wayneodonnell.mp3wallpaper.models.SongInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import lib.folderpicker.FolderPicker;

import static android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM;
import static android.media.MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    @BindView(R.id.btnSetWallpaper)  ImageButton mBtnSetWallpaper;
    @BindView(R.id.btnPrev)    ImageButton mBtnPrev;
    @BindView(R.id.btnNext)  ImageButton mBtnNext;
    @BindView(R.id.imageView) ImageView mImageView;
    @BindView(R.id.imageViewBackground) ImageView mImageViewBackground;
    @BindView(R.id.btnCollage)  ImageButton mBtnCollage;
    @BindView(R.id.txtAlbum)     TextView mTxtAlbum;
    @BindView(R.id.txtArtist)     TextView mTxtArtist;
    @BindView(R.id.btnFavourite)     ImageButton mBtnFavourite;
    @BindView(R.id.btnBlacklist)     ImageButton mBtnBlacklist;
    @BindView(R.id.txtHeader)     TextView mTxtHeader;

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    private Menu menu;

    ArrayList<String> mFileList=new ArrayList<String>();
    ArrayList<String> mAlbumList=new ArrayList<String>();
    ArrayList<String> mBlacklist=new ArrayList<String>();
    ArrayList<String> mFilterList=new ArrayList<String>();
    ArrayList<String> mFavouriteList=new ArrayList<String>();

    int position=-99; //Start as -99 as this will never be reached again in code
    int favPosition=-1;
    int blPosition=-1;
    int heldPosition=-99;
    int heldFavPosition=-99;
    int heldBlPosition=-99;
    int next=1;
    int prev=-1;
    boolean onlyFavourites=false;
    boolean onlyBlacklist=false;
    String currentPath="";
    String currentAlbum="";
    String currentArtist="";
    boolean inSearch=false;
    boolean searchActive=false;
    int FOLDERPICKER_CODE=1;
    String startFolder="";

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
        mBtnFavourite.setOnClickListener(this);
        mBtnBlacklist.setOnClickListener(this);
        loadSavedLists();
        getCurrentPaper(); //Populate screen with current system wallpaper
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FOLDERPICKER_CODE && resultCode == Activity.RESULT_OK) {
            String folderLocation = intent.getExtras().getString("data");
            addToSharedPreferences(Constants.PREFERENCES_STARTFOLDER,folderLocation);
            getFileList(); //Refresh file list with new folder
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_filter, menu);
        ButterKnife.bind(this);
        this.menu=menu;
        MenuItem menuItemFilter = menu.findItem(R.id.action_filter);
        SearchView searchView = (SearchView) menuItemFilter.getActionView();
        final MenuItem menuItemFavorites = menu.findItem(R.id.action_favourites);
        final MenuItem menuItemBlacklist = menu.findItem(R.id.action_blacklist);
        final MenuItem menuItemRefresh = menu.findItem(R.id.action_refresh);
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchActive = true;
                menuItemFavorites.setVisible(false);
                menuItemBlacklist.setVisible(false);
                menuItemRefresh.setVisible(false);
            }
        });

        //Add listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                if(onlyFavourites) {
                    if(heldFavPosition==-99) {
                        heldFavPosition = favPosition; //Store current position
                    }
                }
                else if(onlyBlacklist){
                    if(heldBlPosition==-99) {
                        heldBlPosition = blPosition; //Store current position
                    }
                }
                else {
                    if(heldPosition==-99) {
                        heldPosition = position; //Store current position
                    }
                }
                getFilterList(query);

                if(mFilterList.size()==0){
                    Toast.makeText(MainActivity.this,"Nothing found for '"+query+"'",Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchActive=true;
                return false;
            }

        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchActive=false;
                menuItemFavorites.setVisible(true);
                menuItemBlacklist.setVisible(true);
                menuItemRefresh.setVisible(true);
                mFilterList.clear(); //Clear the filter

                if(onlyFavourites) {
                    if(heldFavPosition>-99) {
                        favPosition= heldFavPosition+1;  //Store current position
                    }
                    else {
                        favPosition+=1;
                    }
                }
                else if(onlyBlacklist){
                    if(heldBlPosition>-99) {
                        blPosition= heldBlPosition+1; //Store current position
                    }
                    else {
                        blPosition+=1;
                    }
                }
                else {
                    if(heldPosition>-99) {
                        position=heldPosition+1; //Store current position
                    }
                    else {
                        position+=1;
                    }
                }

                setImage(prev); //Restore previous image
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //MenuItem menuItemFavorites = menu.findItem(R.id.action_favourites);
        int id = item.getItemId();
        if (id == R.id.action_favourites) {
            if(!searchActive) {
                if (onlyFavourites) {
                    onlyFavourites = false;
                    favPosition = -1;
                    menu.findItem(R.id.action_favourites).setIcon(R.drawable.baseline_favorite_white_24);
                    //Hide the header
                    mTxtHeader.setVisibility(View.GONE);
                    //Restore previous image
                    if (heldPosition > -99) {
                        position = heldPosition + 1;
                        heldPosition = -99;
                    } else {
                        position += 1;
                    }

                    setImage(prev); //Restore previous image
                } else {
                    if (mFavouriteList.size() == 0) {
                        Toast.makeText(MainActivity.this, R.string.nothingFound, Toast.LENGTH_SHORT).show();
                    } else {
                        heldPosition = position; //Store current position
                        onlyFavourites = true;
                        onlyBlacklist = false; //Can't activate both at once
                        menu.findItem(R.id.action_favourites).setIcon(R.drawable.baseline_thumb_down_white_24);

                        //Show header indicating we are displaying favourites
                        mTxtHeader.setVisibility(View.VISIBLE);
                        mTxtHeader.setBackgroundColor(getColor(R.color.colorFavHeader));
                        mTxtHeader.setTextColor(getColor(R.color.colorFavText));
                        mTxtHeader.setText(R.string.favourites);
                        menu.findItem(R.id.action_favourites).setIcon(R.drawable.baseline_favorite_black_24);
                        //Display the first favourite
                        favPosition = -1;
                        setImage(next);
                    }
                }
            }
            return true;
        }
        if (id == R.id.action_blacklist) {
            if(!searchActive) {
                if (onlyBlacklist) {
                    onlyBlacklist = false;
                    blPosition = -99;
                    menu.findItem(R.id.action_blacklist).setIcon(R.drawable.baseline_thumb_down_white_24);
                    //Hide the header
                    mTxtHeader.setVisibility(View.GONE);
                    //Restore previous image
                    if (heldPosition > -99) {
                        position = heldPosition + 1;
                        heldPosition = -99;
                    } else {
                        position += 1;
                    }

                    setImage(prev); //Restore previous image
                } else {
                    if (mBlacklist.size() == 0) {
                        Toast.makeText(MainActivity.this, R.string.nothingFound, Toast.LENGTH_SHORT).show();
                    } else {
                        heldPosition = position; //Store current position
                        onlyBlacklist = true;
                        onlyFavourites = false; //Can't activate both at once
                        menu.findItem(R.id.action_favourites).setIcon(R.drawable.baseline_favorite_white_24);

                        //Show header indicating we are displaying favourites
                        mTxtHeader.setVisibility(View.VISIBLE);
                        mTxtHeader.setBackgroundColor(getColor(R.color.colorBlacklistHeader));
                        mTxtHeader.setTextColor(getColor(R.color.colorBlacklistText));
                        mTxtHeader.setText(R.string.blacklist);
                        menu.findItem(R.id.action_blacklist).setIcon(R.drawable.baseline_thumb_down_black_24);
                        blPosition = -1;
                        setImage(next);
                    }
                    return true;
                }
            }
        }

        // Reload list of files
        if (id == R.id.action_refresh) {
            getFileListOption();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View v){
        if(v==mBtnSetWallpaper){
            setPaper(mImageView);
        }
        if(v==mBtnCollage)
        {
            mImageView.setImageBitmap(createCollage());
            mImageViewBackground.setImageDrawable(mImageView.getDrawable());
        }

        if(v==mBtnNext){
            setImage(next);
            //Ensure buttons are visible in case they were removed by collage
            mBtnFavourite.setVisibility(View.VISIBLE);
            mBtnBlacklist.setVisibility(View.VISIBLE);
        }
        if(v==mBtnPrev){
            setImage(prev);
            //Ensure buttons are visible in case they were removed by collage
            mBtnFavourite.setVisibility(View.VISIBLE);
            mBtnBlacklist.setVisibility(View.VISIBLE);
        }
        if(v==mBtnFavourite){
            //If the path is not in the Favourites list then add, otherwise remove.
            //Also toggle the icon
            if(mFavouriteList.contains(currentPath)){
                mFavouriteList.remove(currentPath);
                mBtnFavourite.setImageResource(R.drawable.baseline_favorite_white_24);
            }
            else{
                mFavouriteList.add(currentPath);
                mBtnFavourite.setImageResource(R.drawable.baseline_favorite_black_24);
            }
            Collections.sort(mFavouriteList);
            saveFile(Constants.FAVOURITES_FILENAME,mFavouriteList);
        }
        if(v==mBtnBlacklist){
            //If the path is not in the Blacklist then add, otherwise remove.
            //            //Also toggle the icon
            if(mBlacklist.contains(currentPath)){
                mBlacklist.remove(currentPath);
                mBtnBlacklist.setImageResource(R.drawable.baseline_thumb_down_white_24);
            }
            else{
                mBlacklist.add(currentPath);
                mBtnBlacklist.setImageResource(R.drawable.baseline_thumb_down_black_24);
            }
            Collections.sort(mBlacklist);
            saveFile(Constants.BLACKLIST_FILENAME,mBlacklist);
        }
    }

    public void getFilterList(String query){
        //Add each file, add name to the list
        if(!inSearch) {
            inSearch=true;
            query = query.toLowerCase();
            mFilterList.clear();
            String albumPath;

            //Determine which list to search in
            if (onlyFavourites) {
                for (String item : mFavouriteList) {
                    //Check each entry in list and add to filter if specified text appears.
                    albumPath = item.toLowerCase().substring(0, item.toLowerCase().lastIndexOf("/"));
                    if (albumPath.contains(query)) {
                        mFilterList.add(item);
                    }
                }
            } else if (onlyBlacklist) {
                for (String item : mBlacklist) {
                    //Check each entry in list and add to filter if specified text appears.
                    albumPath = item.toLowerCase().substring(0, item.toLowerCase().lastIndexOf("/"));
                    if (albumPath.contains(query)) {
                        mFilterList.add(item);
                    }
                }
            } else {
                for (String item : mFileList) {
                    //Check each entry in list and add to filter if specified text appears.
                    albumPath = item.toLowerCase().substring(0, item.toLowerCase().lastIndexOf("/"));
                    if (albumPath.contains(query)) {
                        mFilterList.add(item);
                    }
                }
            }

            if (mFilterList.size() > 0) {
                //Refresh the list being used
                position = -1;
                setImage(next);
            }
            inSearch=false;
        }
    }
    public void getFileListOption(){
        startFolder = mSharedPreferences.getString(Constants.PREFERENCES_STARTFOLDER, null);
        if(TextUtils.isEmpty(startFolder)){
            setStartFolder();
        }
        else {
            String[] options = {"Refresh '" + startFolder+ "'", "Select new folder", "Cancel"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Refresh files");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int params = -1;
                    if (which == 0) {
                        getFileList();
                    } else if (which == 1) {
                        setStartFolder();
                    }
                    dialog.cancel();
                }
            });
            builder.show();
        }
    }
    public void getFileList() {
        //TODO - Add loading spinner - currently struggling to get this to show
        //Before accessing files, need to check permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // If permission is not granted need to request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    Constants.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }

        //Retrieve startfolder from shared preferences
        startFolder = mSharedPreferences.getString(Constants.PREFERENCES_STARTFOLDER, null);
        if(!TextUtils.isEmpty(startFolder)){

            List<File> files = getListFiles(new File(startFolder));

            //Add each file, add name to the list
            String albumPath;
            String songPath;
            mAlbumList.clear();
            mFileList.clear();
            mFilterList.clear();
            for (File file : files) {
                //Only add if the album hasn't already been added and album isn't on blacklist
                songPath = file.toString();
                albumPath = songPath.substring(0, songPath.lastIndexOf("/"));
                if (!mAlbumList.contains(albumPath)) {
                    mAlbumList.add(albumPath);
                    mFileList.add(file.toString());
                }
            }
            //Collections.shuffle(mFileList,new Random());
            //Sort the list into order
            Collections.sort(mFileList);
            //Save the list to a file
            saveFile(Constants.ALBUMS_FILENAME, mFileList);
            Toast.makeText(MainActivity.this, R.string.listRefreshed, Toast.LENGTH_SHORT).show();
        }
        else {
            setStartFolder();
        }
    }
    public void setStartFolder(){
        //String startFolder="/storage/C219-D78B/Music/";
        Intent intent = new Intent(MainActivity.this, FolderPicker.class);
        String startFolder = mSharedPreferences.getString(Constants.PREFERENCES_STARTFOLDER, null);
        if(!TextUtils.isEmpty(startFolder)){
            intent.putExtra("location", startFolder);
        }
        startActivityForResult(intent, FOLDERPICKER_CODE);
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
        //If only looking at blacklist
        else if(onlyBlacklist){
            //Get next from mFavouriteList
            blPosition=setFile(mBlacklist,blPosition,direction);
        }
        //Otherwise use the main file list
        else if(mFileList.size()>1){
            //Get next from mFileList
            position=setFile(mFileList,position,direction);
        }
    }

    public int setFile(ArrayList<String> aList,int pos, int direction){
        String filepath;
        //If position is -99 then we have clicked Prev or next for the first time
        //See if current wallpaper is still available in the array and if so, set its position
        if(aList.size()==0){
            Toast.makeText(MainActivity.this, R.string.nothingFound, Toast.LENGTH_SHORT).show();
        }
        else {
            if (pos == -99) {
                if (aList.contains(currentPath)) {
                    pos = aList.indexOf(currentPath);
                } else {
                    pos = 0;
                }
            }
            //Get next position
            while (true) {
                pos += direction; // Get next position
                if (pos == aList.size() && direction == 1) {
                    pos = 0;
                }
                if (pos == -1 && direction == -1) {
                    pos = aList.size() - 1;
                }

                filepath = aList.get(pos);
                // If not looking at the blacklist then keep moving through the list if the item is blacklisted
                if ((!onlyBlacklist && !mBlacklist.contains(filepath)) || onlyBlacklist) {
                    break;
                }
            }

            if (!TextUtils.isEmpty(filepath)) {
                SongInfo song = new SongInfo(filepath);
                //Get album art from mp3 file
                Bitmap bm = song.getAlbumCover();
                currentPath = filepath; // Set global variable so we always have a record of current path
                currentAlbum = song.getAlbumTitle();
                currentArtist = song.getArtistName();
                //Set ImageView to use bitmap
                if (bm != null) {
                    //Set the images
                    mImageView.setImageBitmap(bm);
                    mImageViewBackground.setImageBitmap(bm);
                }
                else {
                    //No image found - display default
                    mImageView.setImageResource(R.drawable.example);
                    mImageViewBackground.setImageResource(R.drawable.example);
                }
                //Ensure Favourite and Blacklist buttons are available
                //If the image is a favourite then icon should change to indicate this, otherwise should allow to add as favourite
                if(mFavouriteList.contains(filepath)){
                    mBtnFavourite.setImageResource(R.drawable.baseline_favorite_black_24);
                }
                else {
                    mBtnFavourite.setImageResource(R.drawable.baseline_favorite_white_24);
                }
                //If the image is blacklisted then icon should change to indicate this, otherwise should allow to blacklist
                if(mBlacklist.contains(filepath)){
                    mBtnBlacklist.setImageResource(R.drawable.baseline_thumb_down_black_24);
                }
                else {
                    mBtnBlacklist.setImageResource(R.drawable.baseline_thumb_down_white_24);
                }
                updateMeta(currentAlbum, currentArtist);// Show album and artist
            }
        }
        return pos; //Return updated position
    }

    public void getCurrentPaper(){
        //Retrieve album name and artist from shared preferences
        currentPath=mSharedPreferences.getString(Constants.PREFERENCES_LASTPATH,null);
        String albumName=mSharedPreferences.getString(Constants.PREFERENCES_LASTALBUM,null);
        String artistName=mSharedPreferences.getString(Constants.PREFERENCES_LASTARTIST,null);

        if (mFileList.contains(currentPath)) {
            position = mFileList.indexOf(currentPath)-1;
        } else {
            position = 0-1;
        }
        setImage(next);
    }

    public void setPaper(ImageView imageView){
        //Set wallpaper to content of imageView
        String[] options = {"Home screen", "Lock screen", "Home and Lock screens"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Where to set wallpaper?");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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
                        //Store album name and artist to shared preferences
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
        //Ask user where to set wallpaper then set Home screen and/or lock screen accordingly.
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

    private Bitmap createCollage(){
        //Get bitmaps

        //Copy the original list and shuffle the collection
        ArrayList<String> collageArray=new ArrayList<String>(16);
        //Collections.shuffle(collageArray,new Random());

        Bitmap[] collagebitmaps=new Bitmap[16];

        Random rand = new Random();
        int rnd;

        for(int i=0;i<16;i++){
            rnd = rand.nextInt(mFileList.size() - 1);
            //Ensure image not on blacklist and not already in collage
            while(mBlacklist.contains(mFileList.get(rnd)) || collageArray.contains(mFileList.get(rnd)) ){
                rnd = rand.nextInt(mFileList.size() - 1);
            }
            collageArray.add(mFileList.get(rnd));
            //ScaledBitmap not very good quality
            //collagebitmaps[i]= Bitmap.createScaledBitmap(extractAlbumArt(collagelist[i]),100,100,false);
            RenderScript RS = RenderScript.create(this);
            //Get album art from mp3 file
            SongInfo song=new SongInfo(mFileList.get(rnd));
            Bitmap bm=song.getAlbumCover();
            collagebitmaps[i]= resizeBitmap(RS,bm,100);
        }

        Bitmap result = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        for (int i = 0; i < collagebitmaps.length; i++) {
            canvas.drawBitmap(collagebitmaps[i], 100 * (i % 4), 100 * (i / 4), paint);
        }
        updateMeta("Collage","Various artists");

        //When showing collage, don't show Favourite and Blacklist icons
        mBtnFavourite.setVisibility(View.GONE);
        mBtnBlacklist.setVisibility(View.GONE);

        return result;
    }

    public static Bitmap resizeBitmap(RenderScript rs, Bitmap src, int dstWidth) {
        //TODO - see if resizeBitmap can be changed to centre image if not exactly square
        Bitmap.Config  bitmapConfig = src.getConfig();
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        float srcAspectRatio = (float) srcWidth / srcHeight;
        int dstHeight = (int) (dstWidth / srcAspectRatio);

        float resizeRatio=1; //We always want squares in this app.

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
            mTxtAlbum.setText(album);
            mTxtArtist.setText(artist);
        }
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
        if(mFileList.size()==0) {
            getFileList();
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
        //Retrieve favourites
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

    public void saveFile(String filename, ArrayList<String> arrayList){
        String fileContents;
        JSONArray jsonArray=new JSONArray(arrayList);
        fileContents=jsonArray.toString();
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
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

}

//TODO - Landscape orientation - constraint
//TODO - Default image
//TODO - App icon