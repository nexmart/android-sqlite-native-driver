package com.hotwirestudios.sqlite.driver;

/**
 * Provides more control over a connection life-cycle than SQLiteConnection
 */
interface SQLiteConnectionInternal extends SQLiteConnection {
    /**
     * Opens a connection.
     *
     * @throws SQLiteException
     */
    void open() throws SQLiteException;

    /**
     * Closes a connection.
     *
     * @throws SQLiteException
     */
    void close() throws SQLiteException;

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
