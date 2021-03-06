package com.hotwirestudios.sqlite.driver;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bolts.Task;

/**
 * Provides sequential access to a database.
 */
public class DatabaseAccess {
    private final ExecutorService executorService;
    private final NativeSQLiteConnection connection;

    /**
     * Instantiates a new DatabaseAccess, storing or reading data to/from the provided path.
     *
     * @param path The full qualified database path
     */
    public DatabaseAccess(@NonNull String path, @Nullable String key) {
        super();
        executorService = Executors.newSingleThreadExecutor();
        connection = new NativeSQLiteConnection(path, key, SQLiteNative.SQLITE_OPEN_CREATE_IF_NECESSARY | SQLiteNative.SQLITE_OPEN_READWRITE);
    }

    /**
     * Queues and executes the provided context.
     *
     * @param <T>               The result type
     * @param connectionContext The execution context
     * @param withinTransaction If true, a new transaction is started before running the context and automatically committed or rolled back on success/error.
     * @return The resulting asynchronous task
     */
    public <T> Task<T> performThreadsafe(final SQLiteConnectionContext<T> connectionContext, final boolean withinTransaction) {
        return Task.call(new Callable<T>() {
            @Override
            public T call() throws Exception {
                if (!connection.isOpen()) {
                    connection.open();
                }
                connection.executeStatement("PRAGMA foreign_keys = 1");
                try {
                    if (withinTransaction) {
                        connection.beginTransaction();
                    }
                    T result = connectionContext.run(connection);
                    if (withinTransaction) {
                        connection.commitTransaction();
                    }
                    return result;
                } catch (Exception exception) {
                    if (withinTransaction) {
                        connection.rollbackTransaction();
                    }
                    throw exception;
                }
            }
        }, executorService);
    }

    /**
     * Provides an operation to run with a certain connection.
     *
     * @param <TResult> The result type
     */
    public interface SQLiteConnectionContext<TResult> {
        /**
         * Runs operations on the provided connection.
         *
         * @param connection The connection
         * @return The result
         * @throws Exception
         */
        TResult run(@NonNull SQLiteConnection connection) throws Exception;
    }
}
