package com.example.musicplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaMetadataRetriever;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class MediaDataManager {
    private MusicDBHelper dbHelper;
    Context context;

    public MediaDataManager(Context context) {
        dbHelper = new MusicDBHelper(context);
        this.context=context;
    }

    public void insertTrack(Track track) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MusicDBHelper.COL_PATH, track.getPath());
        values.put(MusicDBHelper.COL_TITLE, track.getTitle());
        values.put(MusicDBHelper.COL_ARTIST, track.getArtist());
        values.put(MusicDBHelper.COL_ALBUM, track.getAlbum());
        values.put(MusicDBHelper.COL_GENRE, track.getGenre());
        values.put(MusicDBHelper.COL_DURATION, track.getDuration());
        values.put(MusicDBHelper.COL_ALBUM_ART_PATH, track.getAlbumArtPath());
        values.put(MusicDBHelper.COL_CD_TRACK_NUMBER, track.getCdTrackMumber());
        db.insertWithOnConflict(MusicDBHelper.TABLE_TRACKS, null, values,
                                SQLiteDatabase.CONFLICT_IGNORE);
    }
    public int getCountDb() {
        return dbHelper.getReadableDatabase().query(MusicDBHelper.TABLE_TRACKS, null, null, null, null, null,
                                 null).getCount();
    }
    public List<Track> getAllTracks() {
        List<Track> tracks = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(MusicDBHelper.TABLE_TRACKS, null, null, null, null, null,
                                 MusicDBHelper.COL_TITLE + " COLLATE NOCASE ASC");
        if (cursor.moveToFirst()) {
            do { if(cursorToTrack(cursor)!=null) tracks.add(cursorToTrack(cursor)); } while (cursor.moveToNext());
        }
        cursor.close();
        return tracks;
    }

    public List<String> getAlbums() {
        List<String> albums = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(true, MusicDBHelper.TABLE_TRACKS,
                                 new String[]{MusicDBHelper.COL_ALBUM},
                                 null, null, null, null,
                                 MusicDBHelper.COL_ALBUM + " COLLATE NOCASE ASC", null);
        if (cursor.moveToFirst()) {
            do {
                albums.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return albums;
    }

    public List<String> getArtists() {
        List<String> artists = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(true, MusicDBHelper.TABLE_TRACKS,
                                 new String[]{MusicDBHelper.COL_ARTIST},
                                 null, null, null, null,
                                 MusicDBHelper.COL_ARTIST + " COLLATE NOCASE ASC", null);
        if (cursor.moveToFirst()) {
            do {
                artists.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return artists;
    }

    public List<String> getGenres() {
        List<String> genres = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(true, MusicDBHelper.TABLE_TRACKS,
                                 new String[]{MusicDBHelper.COL_GENRE},
                                 null, null, null, null,
                                 MusicDBHelper.COL_GENRE + " COLLATE NOCASE ASC", null);
        if (cursor.moveToFirst()) {
            do {
                genres.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return genres;
    }

    public List<Track> getTracksByAlbum(String album) {
        List<Track> tracks = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(MusicDBHelper.TABLE_TRACKS, null,
                                 MusicDBHelper.COL_ALBUM + "=?", new String[]{album},
                                 null, null, MusicDBHelper.COL_TITLE + " ASC");
        if (cursor.moveToFirst()) {
            do {if(cursorToTrack(cursor)!=null) tracks.add(cursorToTrack(cursor)); } while (cursor.moveToNext());
        }
        cursor.close();
        return tracks;
    }

    public List<Track> getTracksByArtist(String artist) {
        List<Track> tracks = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(MusicDBHelper.TABLE_TRACKS, null,
                                 MusicDBHelper.COL_ARTIST + "=?", new String[]{artist},
                                 null, null, MusicDBHelper.COL_ALBUM + ", " + MusicDBHelper.COL_TITLE + " ASC");
        if (cursor.moveToFirst()) {
            do {if(cursorToTrack(cursor)!=null) tracks.add(cursorToTrack(cursor)); } while (cursor.moveToNext());
        }
        cursor.close();
        return tracks;
    }
    public List<Track> getTracksByGenre(String genre) {
        List<Track> tracks = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(MusicDBHelper.TABLE_TRACKS, null,
                                 MusicDBHelper.COL_GENRE + "=?", new String[]{genre},
                                 null, null, MusicDBHelper.COL_TITLE + " ASC");
        if (cursor.moveToFirst()) {
            do {if(cursorToTrack(cursor)!=null) tracks.add(cursorToTrack(cursor)); } while (cursor.moveToNext());
        }
        cursor.close();
        return tracks;
    }
    public void clearDatabase() {
        //try{
        //SQLiteDatabase db = dbHelper.getWritableDatabase();
        //db.delete(MusicDBHelper.TABLE_TRACKS, null, null);
        //} catch (Exception e) {
            //delete file - full update
            //LogUtil.logToFile(e);
            context.deleteDatabase(MusicDBHelper.DB_NAME);
         //}
    }

    public static Track extractMetadata(String filePath) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(filePath);
            String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            String durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String trackNumStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            long duration = 0;
            if (durStr != null) duration = Long.parseLong(durStr);
            if (title == null || title.isEmpty()) {
                title = new File(filePath).getName();
            }
            
            int trackNum=0;
            if(trackNumStr!=null){
                trackNumStr=trackNumStr.replace("/","");
                trackNum = Integer.parseInt(trackNumStr);
            }
            LogUtil.logToFile("tn="+trackNum);
            Track t = new Track(filePath, (title==null)?"n/a":title, (artist==null)?"n/a":artist,(album==null)?"n/a":album,(genre==null)?"n/a":genre,trackNum);
            t.setDuration(duration);
            return t;
        } catch (Exception e) {
            LogUtil.logToFile(e);
            return new Track(filePath, new File(filePath).getName(), "", "","",0);
        } finally {
            try{
            mmr.release();
            }catch(Exception e){}
        }
    }

    private Track cursorToTrack(Cursor c) {
        try{
        File f =new File(c.getString(c.getColumnIndex(MusicDBHelper.COL_PATH)));
        if(f.exists()&&f.canRead()){}else{
            //delete
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int i= db.delete(MusicDBHelper.TABLE_TRACKS,MusicDBHelper.COL_PATH + "=?",new String[]{c.getString(c.getColumnIndex(MusicDBHelper.COL_PATH))});
            LogUtil.logToFile("value deleted.."+i);
            return null;
        }
        }catch(Exception e){
            LogUtil.logToFile(e);
            return null;
        }
        try{
        Track t = new Track();
        t.setId(c.getLong(c.getColumnIndex(MusicDBHelper.COL_ID)));
        t.setPath(c.getString(c.getColumnIndex(MusicDBHelper.COL_PATH)));
        t.setTitle(c.getString(c.getColumnIndex(MusicDBHelper.COL_TITLE)));
        t.setArtist(c.getString(c.getColumnIndex(MusicDBHelper.COL_ARTIST)));
        t.setAlbum(c.getString(c.getColumnIndex(MusicDBHelper.COL_ALBUM)));
        t.setGenre(c.getString(c.getColumnIndex(MusicDBHelper.COL_GENRE)));
        t.setDuration(c.getLong(c.getColumnIndex(MusicDBHelper.COL_DURATION)));
        t.setAlbumArtPath(c.getString(c.getColumnIndex(MusicDBHelper.COL_ALBUM_ART_PATH)));
        t.setCdTrackMumber(c.getInt(c.getColumnIndex(MusicDBHelper.COL_CD_TRACK_NUMBER)));
        return t;
        }catch(Exception e){
            LogUtil.logToFile(e);
        }
        return null;
    }
}
