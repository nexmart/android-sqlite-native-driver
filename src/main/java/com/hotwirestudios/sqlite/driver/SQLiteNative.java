package com.hotwirestudios.sqlite.driver;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.ByPtrPtr;
import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.annotation.Name;
import org.bytedeco.javacpp.annotation.Opaque;
import org.bytedeco.javacpp.annotation.Platform;

/**
 * Created by FabianM on 14.06.16.
 */
@Platform(cinclude = "sqlite3.h", link = "sqlite-native-driver")
public class SQLiteNative {
    static {
        Loader.load();
        sqlite3_initialize();
    }

    static final int SQLITE_OPEN_READONLY = 0x00001;
    static final int SQLITE_OPEN_READWRITE = 0x00002;
    static final int SQLITE_OPEN_CREATE = 0x00004;
    static final int SQLITE_OPEN_URI = 0x00040;
    static final int SQLITE_OPEN_MEMORY = 0x00080;
    static final int SQLITE_OPEN_NOMUTEX = 0x08000;
    static final int SQLITE_OPEN_FULLMUTEX = 0x10000;
    static final int SQLITE_OPEN_SHAREDCACHE = 0x20000;
    static final int SQLITE_OPEN_PRIVATECACHE = 0x40000;

    static final int SQLITE_STATIC = 0;
    static final int SQLITE_TRANSIENT = -1;

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

    static native int sqlite3_initialize();

    static native int sqlite3_open_v2(String path, @ByPtrPtr ConnectionHandle connection, int flags, String zVfs);

    static native int sqlite3_close(ConnectionHandle connection);

    static native int sqlite3_prepare_v2(ConnectionHandle connection, String sql, int nByte, @ByPtrPtr StatementHandle statement, @ByPtrPtr String unused);

    static native int sqlite3_step(StatementHandle statement);

    static native int sqlite3_reset(StatementHandle statement);

    static native int sqlite3_clear_bindings(StatementHandle statement);

    static native int sqlite3_bind_parameter_index(StatementHandle statement, String name);

    static native int sqlite3_bind_null(StatementHandle statement, int index);

    static native int sqlite3_bind_int(StatementHandle statement, int index, int value);

    static native int sqlite3_bind_int64(StatementHandle statement, int index, long value);

    static native int sqlite3_bind_text(StatementHandle statement, int index, String value, int nBytes, @Cast("sqlite3_destructor_type") int destructorBehavior);

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
}
