package com.hotwirestudios.sqlite.driver;

/**
 * Created by FabianM on 18.05.16.
 */
public interface SQLiteResultHandler {
    void handleResultCode(@NativeSQLiteConnection.SQLiteResult int code, @NativeSQLiteConnection.SQLiteResult int expected) throws SQLiteException;
    void throwExceptionWithCode(@NativeSQLiteConnection.SQLiteResult int code) throws SQLiteException;
}
