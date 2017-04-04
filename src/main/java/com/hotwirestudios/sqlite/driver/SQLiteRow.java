package com.hotwirestudios.sqlite.driver;

import java.util.Date;

/**
 * Allows accessing the contents of a SQLite result row.
 */
public interface SQLiteRow {
    /**
     * Gets the row id of the provided column.
     *
     * @param column The name of the key column (either primary or foreign)
     * @return The row id or SQLiteObject.ROW_ID_NONE if the value is NULL.
     * @throws SQLiteException
     */
    long getId(String column) throws SQLiteException;

    /**
     * Gets the provided column's value as an Integer.
     *
     * @param column The column name
     * @return The Integer value
     * @throws SQLiteException
     */
    Integer getInteger(String column) throws SQLiteException;

    /**
     * Gets the provided column's value as an Long.
     *
     * @param column The column name
     * @return The Long value
     * @throws SQLiteException
     */
    Long getLong(String column) throws SQLiteException;

    /**
     * Gets the provided column's value as an Integer.
     *
     * @param column The column name
     * @return true, if equal to 1
     * @throws SQLiteException
     */
    Boolean getBoolean(String column) throws SQLiteException;

    /**
     * Gets the provided column's value as a Date. Date values are expected as seconds since 1970-01-01 00:00 UTC.
     *
     * @param column The column name
     * @return The Date value
     * @throws SQLiteException
     */
    Date getDate(String column) throws SQLiteException;

    /**
     * Gets the provided column's value as a String.
     *
     * @param column The column name
     * @return The String value
     * @throws SQLiteException
     */
    String getText(String column) throws SQLiteException;
}
