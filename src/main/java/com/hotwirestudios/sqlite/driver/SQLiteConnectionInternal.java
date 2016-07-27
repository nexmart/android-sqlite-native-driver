package com.hotwirestudios.sqlite.driver;

/**
 * Created by FabianM on 19.05.16.
 */
interface SQLiteConnectionInternal extends SQLiteConnection {
    void open() throws SQLiteException;

    void close() throws SQLiteException;

    void beginTransaction() throws SQLiteException;

    void commitTransaction() throws SQLiteException;

    void rollbackTransaction() throws SQLiteException;
}
