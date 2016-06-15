package com.hotwirestudios.sqlite.driver;

import java.util.Date;

/**
 * Created by FabianM on 18.05.16.
 */
public interface SQLiteRow {
    long getId(String column) throws SQLiteException;

    Integer getInteger(String column) throws SQLiteException;

    Long getLong(String column) throws SQLiteException;

    Boolean getBoolean(String column) throws SQLiteException;

    Date getDate(String column) throws SQLiteException;

    String getText(String column) throws SQLiteException;
}
