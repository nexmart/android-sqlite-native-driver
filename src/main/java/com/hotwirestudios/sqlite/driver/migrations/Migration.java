package com.hotwirestudios.sqlite.driver.migrations;

import com.hotwirestudios.sqlite.driver.SQLiteConnection;
import com.hotwirestudios.sqlite.driver.SQLiteException;
import com.hotwirestudios.sqlite.driver.SQLiteRow;
import com.hotwirestudios.sqlite.driver.SQLiteStatement;

import java.util.Date;
import java.util.List;

/**
 * Base class for database migrations.
 */
public abstract class Migration {
    private final long migrationId;

    /**
     * Instantiates a new Migration.
     *
     * @param migrationId The unique migration id
     */
    Migration(long migrationId) {
        this.migrationId = migrationId;
    }

    /**
     * Gets the unique migration id.
     *
     * @return The unique migration id
     */
    public long getMigrationId() {
        return migrationId;
    }

    /**
     * Gets the migration name
     *
     * @return The name
     */
    public abstract String getName();

    /**
     * Executes the migration using the provided connection and marks it as completed.
     *
     * @param connection The connection
     * @throws Exception
     */
    public void execute(SQLiteConnection connection) throws Exception {
        executeImpl(connection);
        markCompleted(connection);
    }

    private void markCompleted(SQLiteConnection connection) throws SQLiteException {
        SQLiteStatement statement = connection.createStatement("INSERT INTO migration (id, name, execution_date) VALUES (:id, :name, :execution_date)");
        statement.bindId(migrationId).to(":id");
        statement.bindValue(getName()).to(":name");
        statement.bindValue(new Date()).to(":execution_date");
        statement.execute();
    }

    /**
     * Overwrite this to implement the migration logic.
     *
     * @param connection The connection
     * @throws Exception
     */
    protected abstract void executeImpl(SQLiteConnection connection) throws Exception;

    /**
     * Gets the ids of all migrations already performed in the provided connection.
     *
     * @param connection The connection
     * @return The performed migration ids
     * @throws SQLiteException
     */
    public static List<Long> getPerformedMigrationIds(SQLiteConnection connection) throws SQLiteException {
        SQLiteStatement statement = connection.createStatement("SELECT id FROM migration");
        try {
            return statement.readList(new SQLiteStatement.RowValueCallback<Long>() {
                @Override
                public Long readRow(SQLiteRow row) throws SQLiteException {
                    return row.getId("id");
                }
            });
        } finally {
            statement.finish();
        }
    }
}
