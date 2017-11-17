package com.hotwirestudios.sqlite.driver;

import android.util.Log;

/**
 * Created by FabianM on 18.05.16.
 */
public class SQLiteException extends Exception {

    private final @SQLiteResult int errorCode;

    public SQLiteException(@SQLiteResult int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        Log.d("SQLITE", "Result Code: " + errorCode + ", Message: " + message);
    }

    @SQLiteResult
    public int getErrorCode() {
        return errorCode;
    }
}
