package com.hotwirestudios.sqlite.driver;

import android.support.annotation.Nullable;

/**
 * Allows to create a custom database operations object which operates on the provided connection.
 *
 * @param <TOperations> The database operations type
 */
public interface DatabaseOperationsFactory<TOperations> {

    /**
     * Creates a custom database operations object which operates on the provided connection.
     *
     * @param connection The database connection
     * @return The operations object
     */
    TOperations createOperations(@Nullable SQLiteConnection connection);
}
