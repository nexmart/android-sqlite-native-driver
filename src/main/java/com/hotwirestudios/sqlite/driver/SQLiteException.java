package com.hotwirestudios.sqlite.driver;

import android.util.Log;

/**
 * Created by FabianM on 18.05.16.
 */
public class SQLiteException extends Exception {

    private final @NativeSQLiteConnection.SQLiteResult int errorCode;

    public SQLiteException(@NativeSQLiteConnection.SQLiteResult int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        Log.d("Lernapp/SQLITE", "Result Code: " + errorCode + ", Message: " + message);
    }

    @NativeSQLiteConnection.SQLiteResult
    public int getErrorCode() {
        return errorCode;
    }
}
