package com.hotwirestudios.sqlite.driver;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by FabianM on 18.05.16.
 */
public class NativeSQLiteStatement implements SQLiteStatement, SQLiteRow {

    public static final int INTEGER = 1;
    public static final int FLOAT = 2;
    public static final int TEXT = 3;
    public static final int BLOB = 4;
    public static final int NULL = 5;

    @IntDef(value = {
            INTEGER,
            FLOAT,
            TEXT,
            BLOB,
            NULL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SQLiteType {

    }

    private final SQLiteNative.StatementHandle handle;
    private final SQLiteResultHandler resultHandler;
    private Dictionary<String, Integer> columns;

    public static List<String> splitStatements(String sql) {
        List<String> result = new ArrayList<>();
        for (String s : sql.split(";")) {
            String statement = s.trim();
            if (!statement.isEmpty()) {
                result.add(statement);
            }
        }
        return result;
    }

    public NativeSQLiteStatement(SQLiteNative.StatementHandle handle, @NonNull SQLiteResultHandler resultHandler) {
        this.handle = handle;
        this.resultHandler = resultHandler;
    }

    @Override
    public void execute() throws SQLiteException {
        try {
            step();
        } finally {
            finish();
        }
    }

    @Override
    public void step() throws SQLiteException {
        @SQLiteResult int result = SQLiteNative.sqlite3_step(handle);
        resultHandler.handleResultCode(result, SQLiteNative.RESULT_DONE);
    }

    @Override
    public void finish() throws SQLiteException {
        // Ignore errors for finalize, because finalize repeats error codes of the most recent function call
        SQLiteNative.sqlite3_finalize(handle);
    }

    @Override
    public void resetAndClearBindings() throws SQLiteException {
        // Ignore errors, because the error code of the last sqlite3_step will be repeated, if there was an error
        SQLiteNative.sqlite3_reset(handle);
        SQLiteNative.sqlite3_clear_bindings(handle);
    }

    @Override
    public void bindId(long id, @NonNull String parameter) throws SQLiteException {
        if (id == SQLiteObject.ROW_ID_NONE) {
            bindNull(parameter);
            return;
        }

        @SQLiteResult int result = SQLiteNative.sqlite3_bind_int64(handle, SQLiteNative.sqlite3_bind_parameter_index(handle, parameter), id);
        resultHandler.handleResultCode(result, SQLiteNative.RESULT_OK);
    }

    @Override
    public void bindValue(@Nullable Integer i, @NonNull String parameter) throws SQLiteException {
        if (i == null) {
            bindNull(parameter);
            return;
        }

        @SQLiteResult int result = SQLiteNative.sqlite3_bind_int(handle, SQLiteNative.sqlite3_bind_parameter_index(handle, parameter), i);
        resultHandler.handleResultCode(result, SQLiteNative.RESULT_OK);
    }

    @Override
    public void bindValue(@Nullable Long l, @NonNull String parameter) throws SQLiteException {
        if (l == null) {
            bindNull(parameter);
            return;
        }

        @SQLiteResult int result = SQLiteNative.sqlite3_bind_int64(handle, SQLiteNative.sqlite3_bind_parameter_index(handle, parameter), l);
        resultHandler.handleResultCode(result, SQLiteNative.RESULT_OK);
    }

    @Override
    public void bindValue(@Nullable Boolean b, @NonNull String parameter) throws SQLiteException {
        if (b == null) {
            bindNull(parameter);
            return;
        }

        @SQLiteResult int result = SQLiteNative.sqlite3_bind_int(handle, SQLiteNative.sqlite3_bind_parameter_index(handle, parameter), b ? 1 : 0);
        resultHandler.handleResultCode(result, SQLiteNative.RESULT_OK);
    }

    @Override
    public void bindValue(@Nullable Date date, @NonNull String parameter) throws SQLiteException {
        if (date == null) {
            bindNull(parameter);
            return;
        }

        @SQLiteResult int result = SQLiteNative.sqlite3_bind_int64(handle, SQLiteNative.sqlite3_bind_parameter_index(handle, parameter), TimeUnit.MILLISECONDS.toSeconds(date.getTime()));
        resultHandler.handleResultCode(result, SQLiteNative.RESULT_OK);
    }

    @Override
    public void bindValue(@Nullable String s, @NonNull String parameter) throws SQLiteException {
        if (s == null) {
            bindNull(parameter);
            return;
        }

        @SQLiteResult int result = SQLiteNative.sqlite3_bind_text(handle, SQLiteNative.sqlite3_bind_parameter_index(handle, parameter), s, -1, SQLiteNative.SQLITE_TRANSIENT);
        resultHandler.handleResultCode(result, SQLiteNative.RESULT_OK);
    }

    @Override
    public void bindNull(@NonNull String parameter) throws SQLiteException {
        @SQLiteResult int result = SQLiteNative.sqlite3_bind_null(handle, SQLiteNative.sqlite3_bind_parameter_index(handle, parameter));
        resultHandler.handleResultCode(result, SQLiteNative.RESULT_OK);
    }

    public void load(@NonNull final RowCallback callback, boolean finish) throws SQLiteException {
        load(new RowValueCallback<Void>() {
            @Override
            public Void readRow(SQLiteRow row) throws SQLiteException {
                callback.readRow(row);
                return null;
            }
        }, finish);
    }

    @Override
    public <T> T load(@NonNull final RowValueCallback<T> callback, boolean finish) throws SQLiteException {
        return load(new CancellableRowValueCallback<T>() {
            @Override
            public boolean shouldCancel() {
                return false;
            }

            @Override
            public T readRow(SQLiteRow row) throws SQLiteException {
                return callback.readRow(row);
            }
        }, finish);
    }

    @Override
    public <T> T load(@NonNull CancellableRowValueCallback<T> callback, boolean finish) throws SQLiteException {
        if (callback.shouldCancel()) {
            return null;
        }

        if (columns == null) {
            int count = SQLiteNative.sqlite3_column_count(handle);
            columns = new Hashtable<>(count);
            for (int i = 0; i < count; i++) {
                String name = SQLiteNative.sqlite3_column_name(handle, i);
                columns.put(name, i);
            }
        }

        if (callback.shouldCancel()) {
            return null;
        }

        try {
            @SQLiteResult int result = SQLiteNative.sqlite3_step(handle);
            while (result == SQLiteNative.RESULT_ROW) {
                if (callback.shouldCancel()) {
                    return null;
                }

                T rowResult = callback.readRow(this);
                if (rowResult != null) {
                    return rowResult;
                }
                //noinspection WrongConstant
                result = SQLiteNative.sqlite3_step(handle);
            }
            if (result != SQLiteNative.RESULT_DONE) {
                resultHandler.throwExceptionWithCode(result);
            }
        } finally {
            if (finish) {
                columns = null;
                finish();
            }
        }
        return null;
    }

    @Override
    public <T> List<T> readList(@NonNull final RowValueCallback<T> callback, boolean finish) throws SQLiteException {
        return readList(new CancellableRowValueCallback<T>() {
            @Override
            public boolean shouldCancel() {
                return false;
            }

            @Override
            public T readRow(SQLiteRow row) throws SQLiteException {
                return callback.readRow(row);
            }
        }, finish);
    }

    @Override
    public <T> List<T> readList(final @NonNull CancellableRowValueCallback<T> callback, boolean finish) throws SQLiteException {
        final List<T> result = new ArrayList<>();
        load(new RowCallback() {
            @Override
            public void readRow(SQLiteRow row) throws SQLiteException {
                if (callback.shouldCancel()) {
                    return;
                }

                T item = callback.readRow(row);
                if (item != null) {
                    result.add(item);
                }
            }
        }, finish);
        return result;
    }

    private int getColumnIndex(String name) throws IndexOutOfBoundsException {
        Integer index = columns.get(name);
        if (index == null) {
            throw new IndexOutOfBoundsException();
        }
        return index;
    }

    private boolean isNull(int index) {
        return SQLiteNative.sqlite3_column_type(handle, index) == NULL;
    }

    @Override
    public long getId(String column) throws SQLiteException {
        return SQLiteNative.sqlite3_column_int64(handle, getColumnIndex(column));
    }

    @Override
    public Integer getInteger(String column) throws SQLiteException {
        int index = getColumnIndex(column);
        if (isNull(index)) {
            return null;
        }

        return SQLiteNative.sqlite3_column_int(handle, index);
    }

    @Override
    public Long getLong(String column) throws SQLiteException {
        int index = getColumnIndex(column);
        if (isNull(index)) {
            return null;
        }

        return SQLiteNative.sqlite3_column_int64(handle, getColumnIndex(column));
    }

    @Override
    public Boolean getBoolean(String column) throws SQLiteException {
        int index = getColumnIndex(column);
        if (isNull(index)) {
            return null;
        }

        return SQLiteNative.sqlite3_column_int(handle, index) == 1;
    }

    @Override
    public Date getDate(String column) throws SQLiteException {
        int index = getColumnIndex(column);
        if (isNull(index)) {
            return null;
        }

        long timestamp = SQLiteNative.sqlite3_column_int64(handle, index);
        return new Date(TimeUnit.SECONDS.toMillis(timestamp));
    }

    @Override
    public String getText(String column) throws SQLiteException {
        int index = getColumnIndex(column);
        if (isNull(index)) {
            return null;
        }

        return SQLiteNative.sqlite3_column_text(handle, index);
    }
}
