package com.hotwirestudios.sqlite.driver;

import android.support.annotation.Nullable;

import com.hotwirestudios.sqlite.driver.migrations.Migration;
import com.hotwirestudios.sqlite.driver.migrations.MigrationManager;

import java.util.List;

import bolts.Task;

/**
 * Created by FabianM on 18.05.16.
 */
public class Database<TOperations> {
    private final DatabaseAccess databaseAccess;
    private final DatabaseOperationsFactory<TOperations> factory;
    private final MigrationManager migrationManager;

    public Database(DatabaseAccess databaseAccess, DatabaseOperationsFactory<TOperations> factory) {
        this.databaseAccess = databaseAccess;
        this.factory = factory;
        this.migrationManager = new MigrationManager();
    }

    public <TResult> Task<TResult> performOperations(final OperationsRunner<TOperations, TResult> runner, boolean withinTransaction) {
        return databaseAccess.performThreadsafe(new DatabaseAccess.SQLiteConnectionContext<TResult>() {
            @Override
            public TResult run(@Nullable SQLiteConnection connection) throws Exception {
                TOperations operations = factory.createOperations(connection);
                return runner.run(operations);
            }
        }, true, withinTransaction);
    }

    public Task<Void> migrate(List<Migration> migrations) {
        return migrationManager.executeNecessaryMigrations(migrations, databaseAccess);
    }

    public interface OperationsRunner<TOperations, TResult> {
        TResult run(TOperations operations) throws Exception;
    }
}
