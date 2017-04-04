package com.hotwirestudios.sqlite.driver;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;
import java.util.List;

/**
 * Provides functions to access the most common functions of SQLite statements, adding a little sugar for converting rows into objects, using named parameters and cancelling statements.
 */
public interface SQLiteStatement {
    /**
     * Step and finish a statement.
     *
     * @throws SQLiteException
     */
    void execute() throws SQLiteException;

    /**
     * Calls sqlite3_step.
     *
     * @throws SQLiteException
     */
    void step() throws SQLiteException;

    /**
     * Calls sqlite3_finalize.
     *
     * @throws SQLiteException
     */
    void finish() throws SQLiteException;

    /**
     * Resets the statement and clears existing bindings.
     *
     * @throws SQLiteException
     */
    void resetAndClearBindings() throws SQLiteException;

    /**
     * Binds a row id to a named parameter.
     *
     * @param id        The row id
     * @param parameter The named parameter
     * @throws SQLiteException
     */
    void bindId(long id, @NonNull String parameter) throws SQLiteException;

    /**
     * Binds an Integer value to a named parameter.
     *
     * @param i         The Integer value
     * @param parameter The named parameter
     * @throws SQLiteException
     */
    void bindValue(@Nullable Integer i, @NonNull String parameter) throws SQLiteException;

    /**
     * Binds a Long value to a named parameter.
     *
     * @param l         The Long value
     * @param parameter The named parameter
     * @throws SQLiteException
     */
    void bindValue(@Nullable Long l, @NonNull String parameter) throws SQLiteException;

    /**
     * Binds a Boolean value to a named parameter.
     *
     * @param b         The Boolean value
     * @param parameter The named parameter
     * @throws SQLiteException
     */
    void bindValue(@Nullable Boolean b, @NonNull String parameter) throws SQLiteException;

    /**
     * Binds a Date value to a named parameter. Dates are stored as seconds since 1970-01-01 00:00 UTC.
     *
     * @param date      The date value
     * @param parameter The named parameter
     * @throws SQLiteException
     */
    void bindValue(@Nullable Date date, @NonNull String parameter) throws SQLiteException;

    /**
     * Binds a String value to a named parameter.
     *
     * @param s         the s
     * @param parameter The named parameter
     * @throws SQLiteException
     */
    void bindValue(@Nullable String s, @NonNull String parameter) throws SQLiteException;

    /**
     * Binds null to a named parameter.
     *
     * @param parameter The named parameter
     * @throws SQLiteException
     */
    void bindNull(@NonNull String parameter) throws SQLiteException;

    /**
     * Loads data and calls the callback for each resulting row.
     *
     * @param callback The callback
     * @param finish   Set this to true, if the statement should be auto-finished after reading the last row.
     * @throws SQLiteException
     */
    void load(@NonNull RowCallback callback, boolean finish) throws SQLiteException;

    /**
     * Loads data and calls the callback for each resulting row. Use this for statements returning 0 to 1 rows.
     *
     * @param <T>      Result type
     * @param callback The callback
     * @param finish   Set this to true, if the statement should be auto-finished after reading the last row.
     * @return The resulting object or null
     * @throws SQLiteException
     */
    <T> T load(@NonNull RowValueCallback<T> callback, boolean finish) throws SQLiteException;

    /**
     * Loads data and calls the callback for each resulting row. Use this for cancellable statements returning 0 to 1 rows.
     *
     * @param <T>      Result type
     * @param callback The callback
     * @param finish   Set this to true, if the statement should be auto-finished after reading the last row.
     * @return The resulting object or null
     * @throws SQLiteException
     */
    <T> T load(@NonNull CancellableRowValueCallback<T> callback, boolean finish) throws SQLiteException;

    /**
     * Loads data and calls the callback for each resulting row. Use this for statements possibly returning multiple rows.
     *
     * @param <T>      Result type
     * @param callback The callback
     * @param finish   Set this to true, if the statement should be auto-finished after reading the last row.
     * @return The result list
     * @throws SQLiteException
     */
    <T> List<T> readList(@NonNull RowValueCallback<T> callback, boolean finish) throws SQLiteException;

    /**
     * Loads data and calls the callback for each resulting row. Use this for cancellable statements possibly returning multiple rows.
     *
     * @param <T>      Result type
     * @param callback The callback
     * @param finish   Set this to true, if the statement should be auto-finished after reading the last row.
     * @return The resulting object or null
     * @throws SQLiteException
     */
    <T> List<T> readList(@NonNull CancellableRowValueCallback<T> callback, boolean finish) throws SQLiteException;

    /**
     * Provides a function to read a single row without a return value.
     */
    interface RowCallback {
        /**
         * Reads a single database row.
         *
         * @param row The SQLite row
         * @throws SQLiteException
         */
        void readRow(SQLiteRow row) throws SQLiteException;
    }

    /**
     * Provides a function to read a single row, converting it into an object of type T.
     *
     * @param <T> The result type
     */
    interface RowValueCallback<T> {
        /**
         * Reads a single database row and converts it to an object of type T
         *
         * @param row The SQLite row
         * @return The resulting object or null
         * @throws SQLiteException
         */
        T readRow(SQLiteRow row) throws SQLiteException;
    }

    /**
     * Provides a function to read a single row, converting it into an object of type T, if not cancelled.
     *
     * @param <T> The result type
     */
    interface CancellableRowValueCallback<T> extends RowValueCallback<T> {
        /**
         * Determine whether to cancel the current statement or continue reading the next row.
         *
         * @return If true, the statement is cancelled.
         */
        boolean shouldCancel();
    }
}
