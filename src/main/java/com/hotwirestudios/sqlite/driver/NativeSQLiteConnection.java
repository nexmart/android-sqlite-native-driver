package com.hotwirestudios.sqlite.driver;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by FabianM on 18.05.16.
 */
public class NativeSQLiteConnection implements SQLiteConnectionInternal, SQLiteResultHandler {
    public static final int RESULT_OK = 0;
    public static final int RESULT_ERROR = 1;
    public static final int RESULT_INTERNAL = 2;
    public static final int RESULT_PERM = 3;
    public static final int RESULT_ABORT = 4;
    public static final int RESULT_BUSY = 5;
    public static final int RESULT_LOCKED = 6;
    public static final int RESULT_NO_MEMORY = 7;
    public static final int RESULT_READONLY = 8;
    public static final int RESULT_INTERRUPT = 9;
    public static final int RESULT_IO_ERROR = 10;
    public static final int RESULT_CORRUPT = 11;
    public static final int RESULT_NOT_FOUND = 12;
    public static final int RESULT_FULL = 13;
    public static final int RESULT_CANNOT_OPEN = 14;
    public static final int RESULT_LOCK_ERROR = 15;
    public static final int RESULT_EMPTY = 16;
    public static final int RESULT_SCHEMA_CHANGED = 17;
    public static final int RESULT_TOO_BIG = 18;
    public static final int RESULT_CONSTRAINT = 19;
    public static final int RESULT_MISMATCH = 20;
    public static final int RESULT_MISUSE = 21;
    public static final int RESULT_NOT_IMPLEMENTED_LFS = 22;
    public static final int RESULT_ACCESS_DENIED = 23;
    public static final int RESULT_FORMAT = 24;
    public static final int RESULT_RANGE = 25;
    public static final int RESULT_NON_DB_FILE = 26;
    public static final int RESULT_NOTICE = 27;
    public static final int RESULT_WARNING = 28;
    public static final int RESULT_ROW = 100;
    public static final int RESULT_DONE = 101;

    public static final int OPEN_READONLY = SQLiteNative.SQLITE_OPEN_READONLY;
    public static final int OPEN_READWRITE = SQLiteNative.SQLITE_OPEN_READWRITE;
    public static final int CREATE_IF_NECESSARY = SQLiteNative.SQLITE_OPEN_CREATE;

    private final String path;
    private final @OpenFlags int flags;

    private SQLiteNative.ConnectionHandle handle;

    @IntDef(flag = true, value = {
            OPEN_READONLY,
            OPEN_READWRITE,
            CREATE_IF_NECESSARY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OpenFlags {

    }

    @IntDef(value = {
            RESULT_OK,
            RESULT_ERROR,
            RESULT_INTERNAL,
            RESULT_PERM,
            RESULT_ABORT,
            RESULT_BUSY,
            RESULT_LOCKED,
            RESULT_NO_MEMORY,
            RESULT_READONLY,
            RESULT_INTERRUPT,
            RESULT_IO_ERROR,
            RESULT_CORRUPT,
            RESULT_NOT_FOUND,
            RESULT_FULL,
            RESULT_CANNOT_OPEN,
            RESULT_LOCK_ERROR,
            RESULT_EMPTY,
            RESULT_SCHEMA_CHANGED,
            RESULT_TOO_BIG,
            RESULT_CONSTRAINT,
            RESULT_MISMATCH,
            RESULT_MISUSE,
            RESULT_NOT_IMPLEMENTED_LFS,
            RESULT_ACCESS_DENIED,
            RESULT_FORMAT,
            RESULT_RANGE,
            RESULT_NON_DB_FILE,
            RESULT_NOTICE,
            RESULT_WARNING,
            RESULT_ROW,
            RESULT_DONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SQLiteResult {

    }

    public NativeSQLiteConnection(String path, @OpenFlags int flags) {
        this.path = path;
        this.flags = flags;
    }

    public void open() throws SQLiteException {
        SQLiteNative.ConnectionHandle pointer = new SQLiteNative.ConnectionHandle();
        @SQLiteResult int result = SQLiteNative.sqlite3_open_v2(path, pointer, flags, null);
        handleResultCode(result, RESULT_OK);
        handle = pointer;
    }

    public void close() throws SQLiteException {
        if (handle == null) {
            return;
        }

        @SQLiteResult int result = SQLiteNative.sqlite3_close(handle);
        if (result != RESULT_OK) {
            throw new SQLiteException(result, getResultMessage(result));
        }
        handle = null;
    }

    @Override
    public SQLiteStatement createStatement(String sql) throws SQLiteException {
        SQLiteNative.StatementHandle statement = new SQLiteNative.StatementHandle();
        @SQLiteResult int result = SQLiteNative.sqlite3_prepare_v2(handle, sql, -1, statement, null);
        handleResultCode(result, RESULT_OK);
        return new NativeSQLiteStatement(statement, this);
    }

    @Override
    public void executeStatement(String sql) throws SQLiteException {
        SQLiteStatement statement = createStatement(sql);
        statement.execute();
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

    private String getResultMessage(@SQLiteResult int code) {
        return SQLiteNative.sqlite3_errstr(code);
    }
}
