package com.hotwirestudios.sqlite.driver.migrations;

import com.hotwirestudios.sqlite.driver.SQLiteConnection;

/**
 * Adds fields to an existing table.
 */
public class AddFieldsMigration extends Migration {
    private final String name;
    private final String table;
    private final Field[] fields;

    public AddFieldsMigration(long migrationId, String name, String table, Field[] fields) {
        super(migrationId);
        this.name = name;
        this.table = table;
        this.fields = fields;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected void executeImpl(SQLiteConnection connection) throws Exception {
        for (Field field : fields) {
            String sql = "ALTER TABLE \"" + table + "\" ADD COLUMN \"" + field.name + "\" " + field.type;
            connection.executeStatement(sql);
        }
    }

    public static class Field {
        private String name;
        private String type;

        public Field(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}
