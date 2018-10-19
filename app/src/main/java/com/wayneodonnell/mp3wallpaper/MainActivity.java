package com.wayneodonnell.mp3wallpaper;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.FragmentManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.ComponentName;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicResize;
import android.renderscript.Type;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.Call;
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
import java.util.Calendar;
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
    @BindView(R.id.btnRandom)  ImageButton mBtnRandom;
    @BindView(R.id.txtAlbum)     TextView mTxtAlbum;
    @BindView(R.id.txtArtist)     TextView mTxtArtist;
    @BindView(R.id.btnFavourite)     ImageButton mBtnFavourite;
    @BindView(R.id.btnBlacklist)     ImageButton mBtnBlacklist;
    @BindView(R.id.txtHeader)     TextView mTxtHeader;

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private ProgressDialog mDialog;
    private Menu menu;

    private final static String APP_PACKAGE = "com.wayneodonnell.mp3wallpaper";
    private final static String CHANNEL_ID = APP_PACKAGE + ".NOTIFICATIONS";
    NotificationManager notificationManager;
    NotificationChannel channel;

    ArrayList<String> mFileList=new ArrayList<String>();
    ArrayList<String> mAlbumList=new ArrayList<String>();
    ArrayList<String> mBlacklist=new ArrayList<String>();
    ArrayList<String> mFilterList=new ArrayList<String>();
    ArrayList<String> mFavouriteList=new ArrayList<String>();
    ArrayList<Integer> mRandomFileArray=new ArrayList<Integer>();
    ArrayList<Integer> mRandomFilterArray=new ArrayList<Integer>();
    ArrayList<Integer> mRandomFavouriteArray=new ArrayList<Integer>();
    ArrayList<Integer> mRandomBlacklistArray=new ArrayList<Integer>();

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
    boolean timerActive;
    int FOLDERPICKER_CODE=1;
    String startFolder="";
    String queryText="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        // set up shared preferences
        mSharedPreferences= PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        mEditor=mSharedPreferences.edit();

        timerActive = mSharedPreferences.getBoolean(Constants.PREFERENCES_TIMERACTIVE,false);

        /* Set onClick listeners */
        mBtnSetWallpaper.setOnClickListener(this);
        mBtnPrev.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);
        mBtnCollage.setOnClickListener(this);
        mBtnRandom.setOnClickListener(this);
        mBtnFavourite.setOnClickListener(this);
        mBtnBlacklist.setOnClickListener(this);
        loadSavedLists();
        getCurrentPaper(); //Populate screen with current system wallpaper
    }

    //Saved state handlers for rotation
    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the state
        // All global variables need saving

        outState.putInt(Constants.STATE_POSITION, position);
        outState.putInt(Constants.STATE_FAVPOSITION, favPosition);
        outState.putInt(Constants.STATE_BLPOSITION, blPosition);
        outState.putInt(Constants.STATE_HELDPOS, heldPosition);
        outState.putInt(Constants.STATE_HELDFAVPOS, heldFavPosition);
        outState.putInt(Constants.STATE_HELDBLPOS, heldBlPosition);
        outState.putBoolean(Constants.STATE_ONLYFAV, onlyFavourites);
        outState.putBoolean(Constants.STATE_ONLYBL, onlyBlacklist);
        outState.putString(Constants.STATE_CURRENTPATH, currentPath);
        outState.putString(Constants.STATE_CURRENTALBUM, currentAlbum);
        outState.putString(Constants.STATE_CURRENTARTIST, currentArtist);
        outState.putBoolean(Constants.STATE_TIMERACTIVE, timerActive);
        outState.putBoolean(Constants.STATE_SEARCHACTIVE, searchActive);
        outState.putString(Constants.STATE_QUERYTEXT, queryText);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Read the state
        position = savedInstanceState.getInt(Constants.STATE_POSITION);
        favPosition = savedInstanceState.getInt(Constants.STATE_FAVPOSITION);
        blPosition = savedInstanceState.getInt(Constants.STATE_BLPOSITION);
        heldPosition = savedInstanceState.getInt(Constants.STATE_HELDPOS);
        heldFavPosition = savedInstanceState.getInt(Constants.STATE_HELDFAVPOS);
        heldBlPosition = savedInstanceState.getInt(Constants.STATE_HELDBLPOS);
        onlyFavourites = savedInstanceState.getBoolean(Constants.STATE_ONLYFAV);
        onlyBlacklist = savedInstanceState.getBoolean(Constants.STATE_ONLYBL);
        currentPath = savedInstanceState.getString(Constants.STATE_CURRENTPATH);
        currentAlbum = savedInstanceState.getString(Constants.STATE_CURRENTALBUM);
        currentArtist = savedInstanceState.getString(Constants.STATE_CURRENTARTIST);
        timerActive = savedInstanceState.getBoolean(Constants.STATE_TIMERACTIVE);
        searchActive = savedInstanceState.getBoolean(Constants.STATE_SEARCHACTIVE);
        queryText = savedInstanceState.getString(Constants.STATE_QUERYTEXT);

        //If displaying favourites or blacklist then set the icons..
        if(onlyFavourites){
            //Show header indicating we are displaying favourites
            mTxtHeader.setVisibility(View.VISIBLE);
            mTxtHeader.setBackgroundColor(getColor(R.color.colorFavHeader));
            mTxtHeader.setTextColor(getColor(R.color.colorFavText));
            mTxtHeader.setText(R.string.favourites);
            favPosition+=1;
        }
        else if(onlyBlacklist){
            //Show header indicating we are displaying blacklist
            mTxtHeader.setVisibility(View.VISIBLE);
            mTxtHeader.setBackgroundColor(getColor(R.color.colorBlacklistHeader));
            mTxtHeader.setTextColor(getColor(R.color.colorBlacklistText));
            mTxtHeader.setText(R.string.blacklist);
            blPosition+=1;
        }
        else {
            position+=1;
        }
        setImage(prev); //Restore previous image
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
        final SearchView searchView = (SearchView) menuItemFilter.getActionView();
        final MenuItem menuItemFavorites = menu.findItem(R.id.action_favourites);
        final MenuItem menuItemBlacklist = menu.findItem(R.id.action_blacklist);
        final MenuItem menuItemTimer= menu.findItem(R.id.action_timer);
        final MenuItem menuItemRefresh = menu.findItem(R.id.action_refresh);

        if(searchActive == true){
            menuItemFavorites.setVisible(false);
            menuItemBlacklist.setVisible(false);
            menuItemTimer.setVisible(false);
            menuItemRefresh.setVisible(false);
            searchView.setQuery(queryText,false);
            int ps=position;
            doQuery(queryText);
            position=ps+1;
            setImage(prev);
        }

        //If timer active then change timer icon appropriately.
        if(timerActive){
            menu.findItem(R.id.action_timer).setIcon(R.drawable.baseline_timer_off_24);
        }
        else{
            menu.findItem(R.id.action_timer).setIcon(R.drawable.baseline_timer_24);
        }

        //If favourites or blacklist already selected then highlight icons
        if(onlyFavourites){
            menu.findItem(R.id.action_favourites).setIcon(R.drawable.baseline_favorite_black_24);
        }
        else if(onlyBlacklist) {
            menu.findItem(R.id.action_blacklist).setIcon(R.drawable.baseline_thumb_down_black_24);
        }
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queryText="";
                searchActive = true;
                menuItemFavorites.setVisible(false);
                menuItemBlacklist.setVisible(false);
                menuItemTimer.setVisible(false);
                menuItemRefresh.setVisible(false);
            }
        });

        //Add listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                queryText=searchView.getQuery().toString();
                doQuery(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchActive=true;
                queryText=newText;
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
        //Set or disable timer
        if (id == R.id.action_timer) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if(timerActive){
                menu.findItem(R.id.action_timer).setIcon(R.drawable.baseline_timer_24);
                timerActive=false;
                notificationManager.cancel(1);
                //Store in shared preferences
                mEditor.putBoolean(Constants.PREFERENCES_TIMERACTIVE,timerActive).apply();
                stopTimer();
            }
            else{
                menu.findItem(R.id.action_timer).setIcon(R.drawable.baseline_timer_off_24);
                timerActive=true;

                //Show notification advising daily updates turned on
                notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    channel = new NotificationChannel(
                            CHANNEL_ID,
                            "mp3Wallpaper",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setDescription("mp3Wallpaper notifications");
                }

                PendingIntent contentIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, new Intent(this.getApplicationContext(), MainActivity.class), 0);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    contentIntent = PendingIntent.getActivity(this.getApplicationContext(), 0,
                            new Intent(this.getApplicationContext(), MainActivity.class).putExtra("importance",
                                    channel.getImportance()).putExtra("channel_id", ""), PendingIntent.FLAG_UPDATE_CURRENT);
                }

                NotificationCompat.Builder notification = new NotificationCompat.Builder(this.getApplicationContext(), CHANNEL_ID)
                        .setContentTitle("mp3Wallpaper")
                        .setContentText(getString(R.string.daily_updates))
                        .setOngoing(true)
                        .setGroup("mp3Wallpaper")
                        .setContentIntent(contentIntent)
                        .setSmallIcon(R.drawable.baseline_music_note_24);
                //.setSubText(notificationMsg);

                notificationManager.notify(1, notification.build());

                //Store in shared preferences
                mEditor.putBoolean(Constants.PREFERENCES_TIMERACTIVE,timerActive).apply();
                startTimer();
            }
        }
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
                        menu.findItem(R.id.action_blacklist).setIcon(R.drawable.baseline_thumb_down_white_24);

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

        if(v==mBtnRandom)
        {
            //Determine which list to use for random image
            if (mFilterList.size() > 0){
                // Get the next entry from the randomArray
                int random=position;
                while(random==position){
                    if(mRandomFilterArray.size()==0) {
                        setLimits(Constants.RANDOM_FILTER);
                    }
                    random = mRandomFilterArray.get(0); //Always get the first entry

                    mRandomFilterArray.remove(0); //Remove the first entry
                }
                position=random+1;
            }
            //If only looking at favourites
            else if(onlyFavourites){
                // Get the next entry from the randomArray
                int random=favPosition;
                while(random==favPosition){
                    if(mRandomFavouriteArray.size()==0) {
                        setLimits(Constants.RANDOM_FAVOURITE);
                    }
                    random = mRandomFavouriteArray.get(0); //Always get the first entry
                    mRandomFavouriteArray.remove(0); //Remove the first entry
                }
                favPosition=random+1;
            }
            //If only looking at blacklist
            else if(onlyBlacklist){
                // Get the next entry from the randomArray
                int random=blPosition;
                while(random==blPosition){
                    if(mRandomBlacklistArray.size()==0) {
                        setLimits(Constants.RANDOM_BLACKLIST);
                    }
                    random = mRandomBlacklistArray.get(0); //Always get the first entry
                    mRandomBlacklistArray.remove(0); //Remove the first entry
                }
                blPosition=random+1;
            }
            //Otherwise use the main file list
            else if(mFileList.size()>1){
                // Get the next entry from the randomArray
                int random=position;
                while(random==position){
                    if(mRandomFileArray.size()==0) {
                        setLimits(Constants.RANDOM_BLACKLIST);
                    }
                    random = mRandomFileArray.get(0); //Always get the first entry
                    mRandomFileArray.remove(0); //Remove the first entry
                }
                position=random+1;
            }

            setImage(prev);
            //Ensure buttons are visible in case they were removed by collage
            mBtnFavourite.setVisibility(View.VISIBLE);
            mBtnBlacklist.setVisibility(View.VISIBLE);
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
            setLimits(Constants.RANDOM_FAVOURITE);
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
            setLimits(Constants.RANDOM_BLACKLIST);
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
                setLimits(Constants.RANDOM_FILTER);
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
                    mImageView.setImageResource(R.drawable.default_album);
                    mImageViewBackground.setImageResource(R.drawable.default_album);
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
        Bitmap.Config  bitmapConfig = src.getConfig();
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int dstHeight = dstWidth; // We want square images to set Height equal to width

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

        //Retrieve blacklist first so that entries can be excluded from further lists
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
                setLimits(Constants.RANDOM_FILE);
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
    public void startTimer(){
        //Start the service
        Calendar cur_cal = Calendar.getInstance();
        cur_cal.setTimeInMillis(System.currentTimeMillis());
        cur_cal.set(Calendar.HOUR_OF_DAY, 23); // Set to 23:59 today then add one minute for midnight
        cur_cal.set(Calendar.MINUTE, 59);
        cur_cal.set(Calendar.SECOND, 00);
        cur_cal.add(Calendar.SECOND, 60);
        //cur_cal.add(Calendar.SECOND, 1); //Start straight away

        AlarmManager alarm_manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        //Start at midnight and repeat every 24 hours
        alarm_manager.setInexactRepeating(AlarmManager.RTC, cur_cal.getTimeInMillis(),AlarmManager.INTERVAL_DAY, pi); //Repeat every day
        //alarm_manager.setInexactRepeating(AlarmManager.RTC, cur_cal.getTimeInMillis(),AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi); //Repeat every 15 minutes

        ComponentName receiver = new ComponentName(MainActivity.this, AlarmReceiver.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        /////////////////////////

        //Set the alarm
        /////////////////////


        Toast.makeText(this, R.string.updatesOn, Toast.LENGTH_LONG).show();
    }

    public void stopTimer() {
        Intent intent = new Intent(MainActivity.this, ServiceClass.class);
        PendingIntent pi = PendingIntent.getService(MainActivity.this, 0, intent, 0);
        AlarmManager alarm_manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarm_manager.cancel(pi);
        pi.cancel();

        ComponentName receiver = new ComponentName(MainActivity.this, AlarmReceiver.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        Toast.makeText(this, R.string.updatesOff, Toast.LENGTH_LONG).show();
    }
    public void doQuery(String query){
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
    }

    public void getFileList() {
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
            //Call as Asynctask
            generateFileList genFileList = new generateFileList(this);
            genFileList.execute(startFolder);
        }
        else {
            setStartFolder();
        }
    }

    private class generateFileList extends AsyncTask<String, Integer, String> {
        private Context ctx;

        public generateFileList (Context context){
            ctx = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(ctx);
            mDialog.setMessage("Processing...");
            mDialog.setCancelable(false);
            //mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mDialog.show();
        }

        @Override
        protected String doInBackground(String... strtFolder) {
            try {

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
                setLimits(Constants.RANDOM_FILE);
                Collections.sort(mFileList);
                //Save the list to a file
                saveFile(Constants.ALBUMS_FILENAME, mFileList);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            mDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mDialog.dismiss();
            Toast.makeText(MainActivity.this, R.string.listRefreshed, Toast.LENGTH_LONG).show();
        }
    }

    public void setLimits(int mode) {
        if(mode==Constants.RANDOM_FILE){
            mRandomFileArray.clear();
            for(int i=0;i<mFileList.size();i++){
                //Exclude anything on blacklist
                if(!mBlacklist.contains(mFileList.get(i))) {
                    mRandomFileArray.add(i);
                }
            }
            //Shuffle the array
            Collections.shuffle(mRandomFileArray,new Random());
        }
        else if(mode==Constants.RANDOM_FILTER){
            mRandomFilterArray.clear();
            for(int i=0;i<mFilterList.size();i++){
                mRandomFilterArray.add(i);
            }
            //Shuffle the array
            Collections.shuffle(mRandomFilterArray,new Random());
        }
        else if(mode==Constants.RANDOM_FAVOURITE){
            mRandomFavouriteArray.clear();
            for(int i=0;i<mFavouriteList.size();i++){
                mRandomFavouriteArray.add(i);
            }
            //Shuffle the array
            Collections.shuffle(mRandomFavouriteArray,new Random());
        }
        else if(mode==Constants.RANDOM_BLACKLIST){
            mRandomBlacklistArray.clear();
            for(int i=0;i<mBlacklist.size();i++){
                mRandomBlacklistArray.add(i);
            }
            //Shuffle the array
            Collections.shuffle(mRandomBlacklistArray,new Random());
        }
    }
}

//TODO - Maybe change the icon
//TODO - allow selection of automatic frequency