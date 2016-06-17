package com.hotwirestudios.sqlite.driver;

/**
 * Created by FabianM on 18.05.16.
 */
public interface SQLiteResultHandler {
    void handleResultCode(@SQLiteResult int code, @SQLiteResult int expected) throws SQLiteException;
    void throwExceptionWithCode(@SQLiteResult int code) throws SQLiteException;
}
