package com.hotwirestudios.sqlite.driver;

import android.util.Log;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.annotation.ByPtrPtr;
import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.annotation.Name;
import org.bytedeco.javacpp.annotation.Opaque;
import org.bytedeco.javacpp.annotation.Platform;

import java.nio.charset.Charset;

/**
 * Created by FabianM on 14.06.16.
 */
@SuppressWarnings({"WeakerAccess", "SameParameterValue", "unused"})
@Platform(include = "sqlite-rapidjson.h", link = {"sqlite-native-driver"})
public class SQLiteNative {
    private static final String TAG = "SQLITE_NATIVE";

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

    static {
        Loader.load();
        sqlite3_initialize();
    }

    public static final int SQLITE_OPEN_READONLY = 0x00001;
    public static final int SQLITE_OPEN_READWRITE = 0x00002;
    public static final int SQLITE_OPEN_CREATE_IF_NECESSARY = 0x00004;
    public static final int SQLITE_OPEN_URI = 0x00040;
    public static final int SQLITE_OPEN_MEMORY = 0x00080;
    public static final int SQLITE_OPEN_NOMUTEX = 0x08000;
    public static final int SQLITE_OPEN_FULLMUTEX = 0x10000;
    public static final int SQLITE_OPEN_SHAREDCACHE = 0x20000;
    public static final int SQLITE_OPEN_PRIVATECACHE = 0x40000;

    static final Pointer SQLITE_STATIC = new StaticPointer(0);
    static final Pointer SQLITE_TRANSIENT = new StaticPointer(-1);

    static final int SQLITE_UTF8 = 1;
    static final int SQLITE_UTF16LE = 2;
    static final int SQLITE_UTF16BE = 3;
    static final int SQLITE_UTF16 = 4;
    @Deprecated
    static final int SQLITE_ANY = 5;
    static final int SQLITE_UTF16_ALIGNED = 8;

    @Opaque
    @Name("sqlite3")
    static class ConnectionHandle extends Pointer {
        static {
            Loader.load();
        }

        /**
         * Default native constructor.
         */
        public ConnectionHandle() {
            super((Pointer) null);
        }

        /**
         * Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}.
         */
        public ConnectionHandle(Pointer p) {
            super(p);
        }
    }

    @Opaque
    @Name("sqlite3_stmt")
    static class StatementHandle extends Pointer {
        static {
            Loader.load();
        }

        /**
         * Default native constructor.
         */
        public StatementHandle() {
            super((Pointer) null);
        }

        /**
         * Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}.
         */
        public StatementHandle(Pointer p) {
            super(p);
        }
    }

    static class CollationNeededCallback extends FunctionPointer {
        static {
            Loader.load();
        }

        protected CollationNeededCallback() {
            allocate();
        }

        private native void allocate();

        public void call(Pointer arg, ConnectionHandle connection, int eTextRep, String collationName) {
            if (collationName == null || !collationName.equalsIgnoreCase("DIACRITIC")) {
                return;
            }

            if (sqlite3_create_collation(connection, collationName, eTextRep, null, new CollationCallback()) != RESULT_OK) {
                Log.w(TAG, "Could not auto-register " + collationName + " collation!");
            }
        }
    }

    static class CollationCallback extends FunctionPointer {
        static {
            Loader.load();
        }

        protected CollationCallback() {
            allocate();
        }

        private native void allocate();

        public int call(@SuppressWarnings("UnusedParameters") Pointer arg, int i, @Cast("const void *") final BytePointer left, int j, @Cast("const void *") BytePointer right) {
            byte[] leftBytes = left == null ? new byte[0] : left.getStringBytes();
            String l = new String(leftBytes, 0, i, Charset.forName("UTF-8"));
            byte[] rightBytes = right == null ? new byte[0] : right.getStringBytes();
            String r = new String(rightBytes, 0, j, Charset.forName("UTF-8"));
            return l.compareToIgnoreCase(r);
        }
    }

    @Opaque
    @Name("sqlite3_value")
    public static class ValueHandle extends Pointer {
        static {
            Loader.load();
        }

        /**
         * Default native constructor.
         */
        public ValueHandle() {
            super((Pointer) null);
        }

        /**
         * Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}.
         */
        public ValueHandle(Pointer p) {
            super(p);
        }
    }

    @Opaque
    @Name("sqlite3_context")
    public static class ContextHandle extends Pointer {
        static {
            Loader.load();
        }

        /**
         * Default native constructor.
         */
        public ContextHandle() {
            super((Pointer) null);
        }

        /**
         * Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}.
         */
        public ContextHandle(Pointer p) {
            super(p);
        }
    }

    static class FunctionCallback extends FunctionPointer {
        static {
            Loader.load();
        }

        private final SQLiteFunction function;

        protected FunctionCallback(SQLiteFunction function) {
            this.function = function;
            allocate();
        }

        private native void allocate();

        public void call(ContextHandle context, int argc, @Cast("sqlite3_value **") PointerPointer<ValueHandle> argv) {
            function.call(context, argc, argv);
        }
    }

    static class PrimaryKeysCallback extends FunctionPointer {
        static {
            Loader.load();
        }

        private final PrimaryKeysCallbackFunction function;

        protected PrimaryKeysCallback(PrimaryKeysCallbackFunction function) {
            this.function = function;
            allocate();
        }

        private native void allocate();

        // TODO: Out param instead of return?
        public @Cast("const char **")
        PointerPointer<BytePointer> call(String table, IntPointer length) {
            String[] pks = function.call(table);
            length.put(pks.length);
            return new PointerPointer<>(pks);
        }
    }

    abstract static class FinalCallback extends FunctionPointer {
        static {
            Loader.load();
        }

        protected FinalCallback() {
            allocate();
        }

        private native void allocate();

        public abstract void call(ContextHandle context);
    }

    static native int sqlite3_initialize();

    static native int sqlite3_open_v2(String path, @ByPtrPtr ConnectionHandle connection, int flags, String zVfs);

    static native int sqlite3_close(ConnectionHandle connection);

    static native int sqlite3_prepare_v2(ConnectionHandle connection, String sql, int nByte, @ByPtrPtr StatementHandle statement, @ByPtrPtr String unused);

    static native int sqlite3_step(StatementHandle statement);

    static native int sqlite3_reset(StatementHandle statement);

    static native int sqlite3_clear_bindings(StatementHandle statement);

    static native int sqlite3_bind_parameter_count(StatementHandle statement);

    static native int sqlite3_bind_parameter_index(StatementHandle statement, String name);

    static native String sqlite3_bind_parameter_name(StatementHandle statement, int index);

    static native int sqlite3_bind_null(StatementHandle statement, int index);

    static native int sqlite3_bind_int(StatementHandle statement, int index, int value);

    static native int sqlite3_bind_int64(StatementHandle statement, int index, long value);

    static native int sqlite3_bind_text(StatementHandle statement, int index, String value, int nBytes, @Cast("sqlite3_destructor_type") Pointer destructorBehavior);

    static native int sqlite3_column_count(StatementHandle statement);

    static native String sqlite3_column_name(StatementHandle statement, int index);

    static native int sqlite3_column_type(StatementHandle statement, int index);

    static native int sqlite3_column_int(StatementHandle statement, int index);

    static native long sqlite3_column_int64(StatementHandle statement, int index);

    static native String sqlite3_column_text(StatementHandle statement, int index);

    static native int sqlite3_finalize(StatementHandle statement);

    static native String sqlite3_errmsg(ConnectionHandle connection);

    static native String sqlite3_errstr(int code);

    static native long sqlite3_last_insert_rowid(ConnectionHandle connection);

    static native int sqlite3_collation_needed(ConnectionHandle connection, Pointer p, CollationNeededCallback callback);

    static native int sqlite3_create_collation(ConnectionHandle connection, String name, int eTextRep, Pointer arg, CollationCallback callback);

    static native int sqlite3_create_function(ConnectionHandle connection, String name, int nArg, int eTextRep, Pointer arg, FunctionCallback func, FunctionCallback step, FinalCallback fin);

    public static native String sqlite3_value_text(ValueHandle value);

    public static native void sqlite3_result_int(ContextHandle context, int result);

    public static native int sqlite3_key(ConnectionHandle connection, @Cast("const void *") BytePointer key, int keyLength);

    static native int sqlite_import_json(ConnectionHandle connection, String json, PrimaryKeysCallback primaryKeysCallback);
}
