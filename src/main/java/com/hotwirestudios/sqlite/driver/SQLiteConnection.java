package com.hotwirestudios.sqlite.driver;

/**
 * Created by FabianM on 18.05.16.
 */
public interface SQLiteConnection {
    SQLiteStatement createStatement(String sql) throws SQLiteException;

    void executeStatement(String sql) throws SQLiteException;

    long getLastInsertRowId();
}
