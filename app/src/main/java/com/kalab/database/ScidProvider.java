package com.kalab.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.kalab.database.ScidProviderMetaData.ScidMetaData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ScidProvider extends ContentProvider {
    private static final String TAG = ScidProvider.class.getSimpleName();
    private static final String DB_FOLDER = "db";
    private static final UriMatcher sUriMatcher;
    private static final int INCOMING_GAME_COLLECTION_URI_INDICATOR = 1;
    private static final int INCOMING_SINGLE_GAME_URI_INDICATOR = 2;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(ScidProviderMetaData.AUTHORITY, "games", INCOMING_GAME_COLLECTION_URI_INDICATOR);
        sUriMatcher.addURI(ScidProviderMetaData.AUTHORITY, "games/#", INCOMING_SINGLE_GAME_URI_INDICATOR);
    }

    private static final int SELECTION_COUNT_BOARD_SEARCH = 3;
    private static final String CURRENT_VERSION_KEY = "currentVersion";

    @Override
    public boolean onCreate() {
        copyAssetsToFilesDir();
        return true;
    }

    private void copyAssetsToFilesDir() {
        final Context context = getContext();
        if (context != null) {
            AssetManager assetManager = context.getAssets();
            if (assetManager != null) {
                try {
                    boolean needsUpdate = !isUpToDate();
                    final String[] files = assetManager.list(DB_FOLDER);
                    if (files != null) {
                        for (String fileName : files) {
                            updateFile(assetManager, fileName, needsUpdate);
                        }
                        if (needsUpdate) {
                            setCurrentVersion();
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }

    private void updateFile(AssetManager assetManager, String fileName, boolean needsUpdate) {
        String strippedFileName = fileName.substring(0, fileName.lastIndexOf("."));
        File outputFile = getFileInFilesDir(strippedFileName);
        if (!outputFile.exists() || needsUpdate) {
            Log.d(TAG, "copying " + fileName);
            InputStream input = null;
            OutputStream output = null;
            try {
                input = assetManager.open(new File(DB_FOLDER, fileName).toString());
                output = new FileOutputStream(outputFile);
                copyFile(input, output);
                input.close();
                output.flush();
                output.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }

    private void setCurrentVersion() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(CURRENT_VERSION_KEY, getCurrentAppVersion());
        editor.commit();
    }

    private boolean isUpToDate() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        return preferences.getString(CURRENT_VERSION_KEY, "").equals(
                getCurrentAppVersion());
    }

    private String getCurrentAppVersion() {
        String result = "";
        try {
            result = getContext().getPackageManager().getPackageInfo(
                    getContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.v(TAG, e.getMessage());
        }
        return result;
    }


    private void copyFile(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case INCOMING_GAME_COLLECTION_URI_INDICATOR:
                return ScidMetaData.CONTENT_TYPE;

            case INCOMING_SINGLE_GAME_URI_INDICATOR:
                return ScidMetaData.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String fileName,
                        String[] selectionArgs, String sortOrder) {
        ScidCursor result;
        switch (sUriMatcher.match(uri)) {
            case INCOMING_GAME_COLLECTION_URI_INDICATOR:
                result = createCursorForGameCollection(projection, fileName, selectionArgs, sortOrder);
                break;
            case INCOMING_SINGLE_GAME_URI_INDICATOR:
                result = createCursorForSingleGame(uri, projection, fileName);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return result;
    }

    private ScidCursor createCursorForSingleGame(Uri uri, String[] projection, String fileName) {
        assertFileNameIsNotNull(fileName);
        String scidFileName = getFileInFilesDir(fileName).getAbsolutePath();
        int startPosition = Integer.parseInt(uri.getLastPathSegment());
        return new ScidCursor(scidFileName, projection, startPosition, 1);
    }

    private void assertFileNameIsNotNull(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException(
                    "The scid file name must be specified as the selection.");
        }
    }

    private File getFileInFilesDir(String fileName) {
        return new File(getContext().getFilesDir(), fileName);
    }

    private ScidCursor createCursorForGameCollection(String[] projection, String fileName, String[] selectionArgs, String sortOrder) {
        ScidCursor result;
        assertFileNameIsNotNull(fileName);
        String scidFileName = getFileInFilesDir(fileName).getAbsolutePath();
        int limit = -1;
        try {
            limit = Integer.valueOf(sortOrder);
        } catch (NumberFormatException e) {
            // ignore
        }
        if (selectionArgs != null && isBoardSearch(selectionArgs)) {
            result = getCursorForBoardSearch(scidFileName, projection, selectionArgs, limit);
        } else {
            result = getCursorForHeaderSearch(scidFileName, projection, selectionArgs, limit);
        }
        return result;
    }

    private boolean isBoardSearch(String[] selectionArgs) {
        return selectionArgs.length == SELECTION_COUNT_BOARD_SEARCH;
    }

    private ScidCursor getCursorForHeaderSearch(String scidFileName, String[] projection, String[] selectionArgs, int limit) {
        return new ScidCursor(scidFileName, projection, 0,
                selectionArgs, limit);
    }

    private ScidCursor getCursorForBoardSearch(String scidFileName, String[] projection, String[] selectionArgs, int limit) {
        return new ScidCursor(scidFileName, projection, 0,
                selectionArgs[0], selectionArgs[1],
                Integer.valueOf(selectionArgs[2]), limit);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // not implemented
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // not implemented
        return 0;
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        // not implemented
        return 0;
    }
}
