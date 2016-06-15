package com.hotwirestudios.sqlite.driver.migrations;

import android.support.annotation.Nullable;

import com.hotwirestudios.sqlite.driver.DatabaseAccess;
import com.hotwirestudios.sqlite.driver.SQLiteConnection;
import com.hotwirestudios.sqlite.driver.SQLiteException;

import java.util.ArrayList;
import java.util.List;

import bolts.Task;

/**
 * Created by FabianM on 18.05.16.
 */
public class MigrationManager {
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
