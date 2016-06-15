package com.hotwirestudios.sqlite.driver.migrations;

import com.hotwirestudios.sqlite.driver.SQLiteConnection;
import com.hotwirestudios.sqlite.driver.SQLiteException;
import com.hotwirestudios.sqlite.driver.SQLiteRow;
import com.hotwirestudios.sqlite.driver.SQLiteStatement;

import java.util.Date;
import java.util.List;

/**
 * Created by FabianM on 18.05.16.
 */
public abstract class Migration {
    private final long migrationId;

    Migration(long migrationId) {
        this.migrationId = migrationId;
    }

    public long getMigrationId() {
        return migrationId;
    }

    public abstract String getName();

    public void execute(SQLiteConnection connection) throws Exception {
        executeImpl(connection);
        markCompleted(connection);
    }

    private void markCompleted(SQLiteConnection connection) throws SQLiteException {
        SQLiteStatement statement = connection.createStatement("INSERT INTO migration (id, name, execution_date) VALUES (:id, :name, :execution_date)");
        statement.bindValue(migrationId, ":id");
        statement.bindValue(getName(), ":name");
        statement.bindValue(new Date(), ":execution_date");
        statement.execute();
    }

    protected abstract void executeImpl(SQLiteConnection connection) throws Exception;

    public static List<Long> getPerformedMigrationIds(SQLiteConnection connection) throws SQLiteException {
        SQLiteStatement statement = connection.createStatement("SELECT id FROM migration");
        return statement.readList(new SQLiteStatement.RowValueCallback<Long>() {
            @Override
            public Long readRow(SQLiteRow row) throws SQLiteException {
                return row.getId("id");
            }
        }, true);
    }
}
