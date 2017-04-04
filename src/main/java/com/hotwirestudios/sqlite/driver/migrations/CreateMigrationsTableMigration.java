package com.hotwirestudios.sqlite.driver.migrations;

import com.hotwirestudios.sqlite.driver.SQLiteConnection;

/**
 * Creates the migration table which is mandatory for all other migrations to work properly.
 */
public class CreateMigrationsTableMigration extends Migration {
    public CreateMigrationsTableMigration(long migrationId) {
        super(migrationId);
    }

    @Override
    public String getName() {
        return "Created Migrations Table";
    }

    @Override
    protected void executeImpl(SQLiteConnection connection) throws Exception {
        String sql = "CREATE TABLE migration (" +
                "id INTEGER NOT NULL PRIMARY KEY," +
                "name TEXT NOT NULL CHECK(length(name) > 0)," +
                "execution_date INTEGER NOT NULL" +
                ")";
        connection.executeStatement(sql);
    }
}
