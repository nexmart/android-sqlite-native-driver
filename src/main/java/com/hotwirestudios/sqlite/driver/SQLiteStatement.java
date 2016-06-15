package com.hotwirestudios.sqlite.driver;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;
import java.util.List;

/**
 * Created by FabianM on 18.05.16.
 */
public interface SQLiteStatement {
    void execute() throws SQLiteException;

    void step() throws SQLiteException;

    void finish() throws SQLiteException;

    void resetAndClearBindings() throws SQLiteException;

    void bindValue(long id, @NonNull String parameter) throws SQLiteException;

    void bindValue(@Nullable Integer i, @NonNull String parameter) throws SQLiteException;

    void bindValue(@Nullable Long l, @NonNull String parameter) throws SQLiteException;

    void bindValue(@Nullable Boolean b, @NonNull String parameter) throws SQLiteException;

    void bindValue(@Nullable Date date, @NonNull String parameter) throws SQLiteException;

    void bindValue(@Nullable String s, @NonNull String parameter) throws SQLiteException;

    void bindNull(@NonNull String parameter) throws SQLiteException;

    void load(@NonNull RowCallback callback, boolean finish) throws SQLiteException;

    <T> T load(@NonNull RowValueCallback<T> callback, boolean finish) throws SQLiteException;

    <T> List<T> readList(@NonNull RowValueCallback<T> callback, boolean finish) throws SQLiteException;

    interface RowCallback {
        void readRow(SQLiteRow row) throws SQLiteException;
    }

    interface RowValueCallback<T> {
        T readRow(SQLiteRow row) throws SQLiteException;
    }
}
