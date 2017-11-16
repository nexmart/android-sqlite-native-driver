package com.hotwirestudios.sqlite.driver;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Allows to create a custom database operations object which operates on the provided connection.
 * @param <TReadOperations>
 * @param <TReadWriteOperations>
 */
public interface DatabaseOperationsFactory<TReadOperations, TReadWriteOperations extends TReadOperations>  {

    /**
     * Creates a custom database operations object which operates on the provided connection.
     *
     * @param connection The database connection
     * @return The operations object
     */
    TReadOperations createReadOperations(@NonNull SQLiteConnection connection);

    TReadWriteOperations createReadWriteOperations(@NonNull SQLiteConnection connection);
}
