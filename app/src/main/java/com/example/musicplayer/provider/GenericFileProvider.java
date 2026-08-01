package com.example.musicplayer.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import java.io.File;
import java.io.FileNotFoundException;
import com.example.musicplayer.LogUtil;
import android.os.CancellationSignal;

public class GenericFileProvider extends ContentProvider {

    @Override
    public boolean onCreate() { return true; }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
                            LogUtil.logToFile(uri.getPath());
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        LogUtil.logToFile(name);
        if (name != null) {
            return android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(name.substring(name.lastIndexOf('.') + 1));
        }
        return "application/octet-stream";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String path = uri.getPath();
        LogUtil.logToFile(path);
        if (TextUtils.isEmpty(path)) throw new FileNotFoundException();
        File file = new File("/" + path.replaceFirst("^/", ""));
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
    
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        String path = uri.getPath();
        LogUtil.logToFile(path);
        return super.openFile(uri, mode, signal);
    }
}
