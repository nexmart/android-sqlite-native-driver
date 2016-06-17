package com.hotwirestudios.sqlite.driver;

/**
 * Created by FabianM on 18.05.16.
 */
public interface SQLiteConnection {
    SQLiteStatement createStatement(String sql) throws SQLiteException;

    void executeStatement(String sql) throws SQLiteException;

    void registerFunction(String name, int numberOfArguments, SQLiteFunction function) throws SQLiteException;

    long getLastInsertRowId();
}
