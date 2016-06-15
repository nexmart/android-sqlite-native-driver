package com.hotwirestudios.sqlite.driver;

import android.support.annotation.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bolts.Task;

/**
 * Created by FabianM on 18.05.16.
 */
public class DatabaseAccess {
    private final ExecutorService executorService;
    private final NativeSQLiteConnection connection;

    public DatabaseAccess(String path) {
        super();
        executorService = Executors.newSingleThreadExecutor();
        connection = new NativeSQLiteConnection(path, NativeSQLiteConnection.CREATE_IF_NECESSARY | NativeSQLiteConnection.OPEN_READWRITE);
    }

    public <T> Task<T> performThreadsafe(final SQLiteConnectionContext<T> connectionContext, final boolean connected, final boolean withinTransaction) {
        return Task.call(new Callable<T>() {
            @Override
            public T call() throws Exception {
                if (connected) {
                    connection.open();
                    connection.executeStatement("PRAGMA foreign_keys = 1");
                }
                try {
                    if (connected && withinTransaction) {
                        connection.beginTransaction();
                    }
                    T result = connectionContext.run(connected ? connection : null);
                    if (connected && withinTransaction) {
                        connection.commitTransaction();
                    }
                    return result;
                } catch (Exception exception) {
                    if (connected && withinTransaction) {
                        connection.rollbackTransaction();
                    }
                    throw exception;
                } finally {
                    if (connected) {
                        connection.close();
                    }
                }
            }
        }, executorService);
    }

    public interface SQLiteConnectionContext<TResult> {
        TResult run(@Nullable SQLiteConnection connection) throws Exception;
    }
}
