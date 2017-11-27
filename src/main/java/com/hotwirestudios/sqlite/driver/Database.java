package com.hotwirestudios.sqlite.driver;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hotwirestudios.sqlite.driver.migrations.Migration;
import com.hotwirestudios.sqlite.driver.migrations.MigrationManager;

import java.util.List;

import bolts.Task;

/**
 * Allows sequential access to a database and restricts usage to the operations type.
 *
 * @param <TReadOperations>
 * @param <TReadWriteOperations>
 */
public class Database<TReadOperations, TReadWriteOperations extends TReadOperations> {
    private final DatabaseAccess databaseAccess;
    private final DatabaseOperationsFactory<TReadOperations, TReadWriteOperations> factory;
    private final MigrationManager migrationManager;

    /**
     * Instantiates a new object
     *
     * @param databaseAccess The DatabaseAccess object
     * @param factory        The operations factory
     */
    public Database(DatabaseAccess databaseAccess, DatabaseOperationsFactory<TReadOperations, TReadWriteOperations> factory) {
        this.databaseAccess = databaseAccess;
        this.factory = factory;
        this.migrationManager = new MigrationManager();
    }

    /**
     * Performs the provided operations runner within the database access and provides exclusive access to a database operations object this way.
     *
     * @param <TResult> The result type
     * @param runner    The runner gaining access to the operations object
     * @return A Task representing the asynchronous operation
     */
    public <TResult> Task<TResult> performReadOnlyOperations(final OperationsRunner<TReadOperations, TResult> runner) {
        return databaseAccess.performThreadsafe(new DatabaseAccess.SQLiteConnectionContext<TResult>() {
            @Override
            public TResult run(@NonNull SQLiteConnection connection) throws Exception {
                TReadOperations operations = factory.createReadOperations(connection);
                return runner.run(operations);
            }
        }, false);
    }

    /**
     * Performs the provided operations runner within the database access and provides exclusive access to a database operations object this way.
     * The operations are wrapped in a transaction which will auto-rollback, if an exception is thrown or auto-commit otherwise.
     *
     * @param runner The runner gaining access to the operations object
     * @param <TResult> The result type
     * @return A Task representing the asynchronous operation
     */
    public <TResult> Task<TResult> performReadWriteOperations(final OperationsRunner<TReadWriteOperations, TResult> runner) {
        return databaseAccess.performThreadsafe(new DatabaseAccess.SQLiteConnectionContext<TResult>() {
            @Override
            public TResult run(@NonNull SQLiteConnection connection) throws Exception {
                TReadWriteOperations operations = factory.createReadWriteOperations(connection);
                return runner.run(operations);
            }
        }, true);
    }

    /**
     * Performs all necessary migrations of the provided list.
     * The migrations will be executed in the provided order.
     * As they are identified by the MigrationId, make sure to have unique ids for all provided migrations.
     *
     * @param migrations The migrations.
     * @return The task representing the asynchronous operation
     */
    public Task<Void> migrate(List<Migration> migrations) {
        return migrationManager.executeNecessaryMigrations(migrations, databaseAccess);
    }

    /**
     * Allows to run multiple database operations provided by the operations object in a synchronous way.
     *
     * @param <TOperations> The operations type
     * @param <TResult>     The result type
     */
    public interface OperationsRunner<TOperations, TResult> {
        /**
         * Runs multiple database operations provided by the operations object in a synchronous way.
         *
         * @param operations The operations object
         * @return The result
         * @throws Exception
         */
        TResult run(TOperations operations) throws Exception;
    }
}
