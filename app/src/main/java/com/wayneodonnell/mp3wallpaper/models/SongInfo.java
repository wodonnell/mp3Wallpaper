package com.wayneodonnell.mp3wallpaper.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import static android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM;
import static android.media.MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST;

public class SongInfo {
    Bitmap albumCover;
    String albumTitle;
    String artistName;

    //public SongInfo() {}

    public SongInfo(String path){
        //Extract the embedded image from the specified file
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(path);
        } catch(Exception e){
            e.printStackTrace();
        }

        byte [] data = mmr.getEmbeddedPicture();

         albumCover=null;

        // convert the byte array to a bitmap
        if(data != null) {
            albumCover = BitmapFactory.decodeByteArray(data, 0, data.length);
        }

        //Get album name and album artist
        albumTitle=mmr.extractMetadata(METADATA_KEY_ALBUM);
        artistName=mmr.extractMetadata(METADATA_KEY_ALBUMARTIST);

    }

    public Bitmap getAlbumCover() { return albumCover; }
    public String getAlbumTitle() {
        return albumTitle;
    }
    public String getArtistName() {
        return artistName;
    }
}
