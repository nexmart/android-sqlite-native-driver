package com.hotwirestudios.sqlite.driver;

/**
 * Represents a connection to aSQLite database
 */
public interface SQLiteConnection {
    /**
     * Creates a new prepared statement.
     *
     * @param sql The SQL string
     * @return The SQLiteStatement
     * @throws SQLiteException
     */
    SQLiteStatement createStatement(String sql) throws SQLiteException;

    /**
     * Prepares and executes a statement
     *
     * @param sql The SQL string
     * @throws SQLiteException
     */
    void executeStatement(String sql) throws SQLiteException;

    /**
     * Registers a callback function with the provided name.
     *
     * @param name              The name of the function to be used in SQL strings
     * @param numberOfArguments The number of expected function arguments
     * @param function          The callback function
     * @throws SQLiteException
     */
    void registerFunction(String name, int numberOfArguments, SQLiteFunction function) throws SQLiteException;

    /**
     * Gets the most recently inserted row id - actually meaning the sqlite3 row id, which is only equal to your primary key, if the table has a INTEGER NOT NULL PRIMARY KEY column.
     *
     * @return The sqlite3 row id
     */
    long getLastInsertRowId();

    /**
     * Gets error message of the most recent sqlite3 call.
     *
     * @return The last error message
     * @throws SQLiteException
     */
    String getLastErrorMessage() throws SQLiteException;

    /**
     * Bulk imports JSON into the database. See: https://github.com/hotwirestudios/android-sqlite-native-driver for the expected format.
     *
     * @param json                The JSON string
     * @param primaryKeysCallback A callback telling the native code which columns should be treated as primary keys (for checking whether to update, delete or insert data)
     * @throws SQLiteException
     */
    void importJson(String json, PrimaryKeysCallbackFunction primaryKeysCallback) throws SQLiteException;
}
