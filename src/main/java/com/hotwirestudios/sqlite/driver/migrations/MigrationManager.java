package com.hotwirestudios.sqlite.driver.migrations;

import android.support.annotation.Nullable;

import com.hotwirestudios.sqlite.driver.DatabaseAccess;
import com.hotwirestudios.sqlite.driver.SQLiteConnection;
import com.hotwirestudios.sqlite.driver.SQLiteException;

import java.util.ArrayList;
import java.util.List;

import bolts.Task;

/**
 * Simple migration manager allow to apply new migrations.
 */
public class MigrationManager {
    /**
     * Performs all necessary migrations of the provided list.
     * The migrations will be executed in the provided order.
     * As they are identified by the MigrationId, make sure to have unique ids for all provided migrations.
     *
     * @param migrations     The migrations.
     * @param databaseAccess The database access
     * @return The task representing the asynchronous operation
     */
    public Task<Void> executeNecessaryMigrations(final List<Migration> migrations, final DatabaseAccess databaseAccess) {
        return databaseAccess.performThreadsafe(new DatabaseAccess.SQLiteConnectionContext<Void>() {
            @Override
            public Void run(@Nullable SQLiteConnection connection) throws Exception {
                List<Long> performedMigrationIds;
                try {
                    performedMigrationIds = Migration.getPerformedMigrationIds(connection);
                } catch (SQLiteException exception) {
                    performedMigrationIds = new ArrayList<>();
                }

                for (Migration migration : migrations) {
                    if (performedMigrationIds.contains(migration.getMigrationId())) {
                        continue;
                    }
                    migration.execute(connection);
                }
                return null;
            }
        }, true, true);
    }
}
