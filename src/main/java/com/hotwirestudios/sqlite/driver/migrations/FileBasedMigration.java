package com.hotwirestudios.sqlite.driver.migrations;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;

import com.hotwirestudios.sqlite.driver.NativeSQLiteStatement;
import com.hotwirestudios.sqlite.driver.SQLiteConnection;
import com.hotwirestudios.sqlite.driver.SQLiteStatement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Performs all statements from a provided SQL file resource as a single migration.
 */
public class FileBasedMigration extends Migration {
    private final String name;
    private final @RawRes int resourceId;
    private final Context context;

    public FileBasedMigration(long migrationId, @NonNull String name, Context context, @RawRes int resourceId) {
        super(migrationId);
        this.name = name;
        this.context = context;
        this.resourceId = resourceId;
    }

    @Override
    public String getName() {
        return name;
    }

    private String readStream(@NonNull InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }

    @Override
    protected void executeImpl(SQLiteConnection connection) throws Exception {
        String sql = readStream(context.getResources().openRawResource(resourceId));
        for (String s : NativeSQLiteStatement.splitStatements(sql)) {
            SQLiteStatement statement = connection.createStatement(s);
            statement.execute();
        }
    }
}
