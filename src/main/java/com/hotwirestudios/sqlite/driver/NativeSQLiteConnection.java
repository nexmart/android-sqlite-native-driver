package com.hotwirestudios.sqlite.driver;

/**
 * Created by FabianM on 18.05.16.
 */
public class NativeSQLiteConnection implements SQLiteConnectionInternal, SQLiteResultHandler {

    private final String path;
    private final @OpenFlags int flags;
    private final SQLiteNative.CollationNeededCallback collationCallback;

    private SQLiteNative.ConnectionHandle handle;

    public NativeSQLiteConnection(String path, @OpenFlags int flags) {
        this.path = path;
        this.flags = flags;
        this.collationCallback = new SQLiteNative.CollationNeededCallback();
    }

    public void open() throws SQLiteException {
        SQLiteNative.ConnectionHandle pointer = new SQLiteNative.ConnectionHandle();
        @SQLiteResult int result = SQLiteNative.sqlite3_open_v2(path, pointer, flags, null);
        handleResultCode(result, SQLiteNative.RESULT_OK);
        handle = pointer;

        @SQLiteResult int collationResult = SQLiteNative.sqlite3_collation_needed(handle, null, collationCallback);
        handleResultCode(collationResult, SQLiteNative.RESULT_OK);
    }

    public void close() throws SQLiteException {
        if (handle == null) {
            return;
        }

        @SQLiteResult int result = SQLiteNative.sqlite3_close(handle);
        if (result != SQLiteNative.RESULT_OK) {
            throw new SQLiteException(result, getResultMessage(result));
        }
        handle = null;
    }

    @Override
    public SQLiteStatement createStatement(String sql) throws SQLiteException {
        SQLiteNative.StatementHandle statement = new SQLiteNative.StatementHandle();
        @SQLiteResult int result = SQLiteNative.sqlite3_prepare_v2(handle, sql, -1, statement, null);
        handleResultCode(result, SQLiteNative.RESULT_OK);
        return new NativeSQLiteStatement(statement, this);
    }

    @Override
    public void executeStatement(String sql) throws SQLiteException {
        SQLiteStatement statement = createStatement(sql);
        statement.execute();
    }

    @Override
    public void registerFunction(String name, int numberOfArguments, SQLiteFunction function) throws SQLiteException {
        @SQLiteResult int result = SQLiteNative.sqlite3_create_function(handle, name, numberOfArguments, SQLiteNative.SQLITE_UTF8, null, new SQLiteNative.FunctionCallback(function), null, null);
        handleResultCode(result, SQLiteNative.RESULT_OK);
    }

    @Override
    public void handleResultCode(@SQLiteResult int code, @SQLiteResult int expected) throws SQLiteException {
        if (code != expected) {
            throwExceptionWithCode(code);
        }
    }

    @Override
    public void throwExceptionWithCode(@SQLiteResult int code) throws SQLiteException {
        throw new SQLiteException(code, getLastErrorMessage());
    }

    @Override
    public void beginTransaction() throws SQLiteException {
        executeStatement("BEGIN TRANSACTION");
    }

    @Override
    public void commitTransaction() throws SQLiteException {
        executeStatement("COMMIT TRANSACTION");
    }

    @Override
    public void rollbackTransaction() throws SQLiteException {
        executeStatement("ROLLBACK TRANSACTION");
    }

    @Override
    public long getLastInsertRowId() {
        return SQLiteNative.sqlite3_last_insert_rowid(handle);
    }

    @Override
    public String getLastErrorMessage() throws SQLiteException {
        return SQLiteNative.sqlite3_errmsg(handle);
    }

    @Override
    public void importJson(String json, PrimaryKeysCallbackFunction primaryKeysCallback) throws SQLiteException {
        @SQLiteResult int result = SQLiteNative.sqlite_import_json(handle, json, new SQLiteNative.PrimaryKeysCallback(primaryKeysCallback));
        handleResultCode(result, SQLiteNative.RESULT_OK);
    }

    private String getResultMessage(@SQLiteResult int code) {
        return SQLiteNative.sqlite3_errstr(code);
    }
}
