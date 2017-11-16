package com.hotwirestudios.sqlite.driver;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
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
    private boolean finished = false;

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
    public void finish() {
        if (finished) {
            return;
        }

        // Ignore errors for finalize, because finalize repeats error codes of the most recent function call
        SQLiteNative.sqlite3_finalize(handle);
        finished = true;
        columns = null;
    }

    @Override
    public void resetAndClearBindings() throws SQLiteException {
        // Ignore errors, because the error code of the last sqlite3_step will be repeated, if there was an error
        SQLiteNative.sqlite3_reset(handle);
        SQLiteNative.sqlite3_clear_bindings(handle);
    }

    @Override
    public BindableValue bindId(long id) throws SQLiteException {
        return id == SQLiteObject.ROW_ID_NONE ? bindNull() : getLongBinder(id);
    }

    @Override
    public BindableValue bindValue(@Nullable Integer i) throws SQLiteException {
        return i == null ? bindNull() : getIntegerBinder(i);
    }

    @Override
    public BindableValue bindValue(@Nullable Long l) throws SQLiteException {
        return l == null ? bindNull() : getLongBinder(l);
    }

    @Override
    public BindableValue bindValue(@Nullable Boolean b) throws SQLiteException {
        return b == null ? bindNull() : getBooleanBinder(b);
    }

    @Override
    public BindableValue bindValue(@Nullable Date date) throws SQLiteException {
        return date == null ? bindNull() : getDateBinder(date);
    }

    @Override
    public BindableValue bindValue(@Nullable String s) throws SQLiteException {
        return s == null ? bindNull() : getStringBinder(s);
    }

    @Override
    public BindableValue bindNull() throws SQLiteException {
        return getNullBinder();
    }

    public void load(@NonNull final RowCallback callback) throws SQLiteException {
        load(new RowValueCallback<Void>() {
            @Override
            public Void readRow(SQLiteRow row) throws SQLiteException {
                callback.readRow(row);
                return null;
            }
        });
    }

    @Override
    public <T> T load(@NonNull final RowValueCallback<T> callback) throws SQLiteException {
        return load(new CancellableRowValueCallback<T>() {
            @Override
            public boolean shouldCancel() {
                return false;
            }

            @Override
            public T readRow(SQLiteRow row) throws SQLiteException {
                return callback.readRow(row);
            }
        });
    }

    @Override
    public <T> T load(@NonNull CancellableRowValueCallback<T> callback) throws SQLiteException {
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
        return null;
    }

    @Override
    public <T> List<T> readList(@NonNull final RowValueCallback<T> callback) throws SQLiteException {
        return readList(new CancellableRowValueCallback<T>() {
            @Override
            public boolean shouldCancel() {
                return false;
            }

            @Override
            public T readRow(SQLiteRow row) throws SQLiteException {
                return callback.readRow(row);
            }
        });
    }

    @Override
    public <T> List<T> readList(final @NonNull CancellableRowValueCallback<T> callback) throws SQLiteException {
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
        });
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
        int index = getColumnIndex(column);
        if (isNull(index)) {
            return SQLiteObject.ROW_ID_NONE;
        }

        return SQLiteNative.sqlite3_column_int64(handle, index);
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
        return new Date(timestamp);
    }

    @Override
    public String getText(String column) throws SQLiteException {
        int index = getColumnIndex(column);
        if (isNull(index)) {
            return null;
        }

        return SQLiteNative.sqlite3_column_text(handle, index);
    }

    @Override
    public void close() throws IOException {
        if (!finished) {
            finish();
        }
    }

    private NullBinder nullBinder;

    private NullBinder getNullBinder() {
        if (nullBinder == null) {
            nullBinder = new NullBinder();
        }
        return nullBinder;
    }

    private class NullBinder implements BindableValue {
        @Override
        public void to(@NonNull String parameter) throws SQLiteException {
            to(SQLiteNative.sqlite3_bind_parameter_index(handle, parameter));
        }

        @Override
        public void to(int parameterIndex) throws SQLiteException {
            @SQLiteResult int result = SQLiteNative.sqlite3_bind_null(handle, parameterIndex);
            resultHandler.handleResultCode(result, SQLiteNative.RESULT_OK);
        }
    }

    private abstract class ValueBinder<T> implements BindableValue {
        private T value;

        protected void setValue(T value) {
            this.value = value;
        }

        @Override
        public void to(@NonNull String parameter) throws SQLiteException {
            to(SQLiteNative.sqlite3_bind_parameter_index(handle, parameter));
        }

        @Override
        public void to(int parameterIndex) throws SQLiteException {
            @SQLiteResult int result = bind(value, parameterIndex);
            resultHandler.handleResultCode(result, SQLiteNative.RESULT_OK);
        }

        @SQLiteResult
        protected abstract int bind(T value, int index);
    }

    private IntegerBinder integerBinder;

    private IntegerBinder getIntegerBinder(Integer value) {
        if (integerBinder == null) {
            integerBinder = new IntegerBinder();
        }
        integerBinder.setValue(value);
        return integerBinder;
    }

    private class IntegerBinder extends ValueBinder<Integer> {
        @Override
        protected int bind(Integer value, int index) {
            return SQLiteNative.sqlite3_bind_int(handle, index, value);
        }
    }

    private LongBinder longBinder;

    private LongBinder getLongBinder(Long value) {
        if (longBinder == null) {
            longBinder = new LongBinder();
        }
        longBinder.setValue(value);
        return longBinder;
    }

    private class LongBinder extends ValueBinder<Long> {
        @Override
        protected int bind(Long value, int index) {
            return SQLiteNative.sqlite3_bind_int64(handle, index, value);
        }
    }

    private DateBinder dateBinder;

    private DateBinder getDateBinder(Date value) {
        if (dateBinder == null) {
            dateBinder = new DateBinder();
        }
        dateBinder.setValue(value);
        return dateBinder;
    }

    private class DateBinder extends ValueBinder<Date> {
        @Override
        protected int bind(Date value, int index) {
            return SQLiteNative.sqlite3_bind_int64(handle, index, value.getTime());
        }
    }

    private StringBinder stringBinder;

    private StringBinder getStringBinder(String value) {
        if (stringBinder == null) {
            stringBinder = new StringBinder();
        }
        stringBinder.setValue(value);
        return stringBinder;
    }

    private class StringBinder extends ValueBinder<String> {
        @Override
        protected int bind(String value, int index) {
            return SQLiteNative.sqlite3_bind_text(handle, index, value, -1, SQLiteNative.SQLITE_TRANSIENT);
        }
    }

    private BooleanBinder booleanBinder;

    private BooleanBinder getBooleanBinder(boolean value) {
        if (booleanBinder == null) {
            booleanBinder = new BooleanBinder();
        }
        booleanBinder.setValue(value);
        return booleanBinder;
    }

    private class BooleanBinder extends ValueBinder<Boolean> {
        @Override
        protected int bind(Boolean value, int index) {
            return SQLiteNative.sqlite3_bind_int(handle, index, value ? 1 : 0);
        }
    }
}
