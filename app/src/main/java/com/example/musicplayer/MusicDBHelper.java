package com.example.musicplayer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MusicDBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "musicplayer.db";
    private static final int VERSION = 1;

    public static final String TABLE_TRACKS = "tracks";
    public static final String COL_ID = "_id";
    public static final String COL_PATH = "path";
    public static final String COL_TITLE = "title";
    public static final String COL_ARTIST = "artist";
    public static final String COL_ALBUM = "album";
    public static final String COL_GENRE = "genre";
    public static final String COL_DURATION = "duration";
    public static final String COL_ALBUM_ART_PATH = "album_art_path";
    public static final String COL_CD_TRACK_NUMBER = "cd_track_number";

    public MusicDBHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        LogUtil.logToFile(db.isWriteAheadLoggingEnabled()+"e"+db.isDbLockedByCurrentThread()+"e");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTracks = "CREATE TABLE " + TABLE_TRACKS + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_PATH + " TEXT UNIQUE NOT NULL, " +
            COL_TITLE + " TEXT, " +
            COL_ARTIST + " TEXT, " +
            COL_ALBUM + " TEXT, " +
            COL_GENRE + " TEXT, " +
            COL_DURATION + " INTEGER, " +
            COL_ALBUM_ART_PATH + " TEXT, " +
            COL_CD_TRACK_NUMBER + " INTEGER)";
        db.execSQL(createTracks);
        
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRACKS);
        onCreate(db);
    }
}

