package com.hotwirestudios.sqlite.driver;

import java.io.Closeable;

/**
 * Provides more control over a connection life-cycle than SQLiteConnection
 */
interface SQLiteConnectionInternal extends SQLiteConnection, Closeable {
    /**
     * Opens a connection.
     *
     * @throws SQLiteException
     */
    void open() throws SQLiteException;

    /**
     * Begins a new transaction.
     *
     * @throws SQLiteException
     */
    void beginTransaction() throws SQLiteException;

    /**
     * Commits the current transaction.
     *
     * @throws SQLiteException
     */
    void commitTransaction() throws SQLiteException;

    /**
     * Rolls back the current transaction.
     *
     * @throws SQLiteException
     */
    void rollbackTransaction() throws SQLiteException;
}
