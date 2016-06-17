package com.hotwirestudios.sqlite.driver;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by FabianM on 17.06.16.
 */
@IntDef(value = {
        SQLiteNative.RESULT_OK,
        SQLiteNative.RESULT_ERROR,
        SQLiteNative.RESULT_INTERNAL,
        SQLiteNative.RESULT_PERM,
        SQLiteNative.RESULT_ABORT,
        SQLiteNative.RESULT_BUSY,
        SQLiteNative.RESULT_LOCKED,
        SQLiteNative.RESULT_NO_MEMORY,
        SQLiteNative.RESULT_READONLY,
        SQLiteNative.RESULT_INTERRUPT,
        SQLiteNative.RESULT_IO_ERROR,
        SQLiteNative.RESULT_CORRUPT,
        SQLiteNative.RESULT_NOT_FOUND,
        SQLiteNative.RESULT_FULL,
        SQLiteNative.RESULT_CANNOT_OPEN,
        SQLiteNative.RESULT_LOCK_ERROR,
        SQLiteNative.RESULT_EMPTY,
        SQLiteNative.RESULT_SCHEMA_CHANGED,
        SQLiteNative.RESULT_TOO_BIG,
        SQLiteNative.RESULT_CONSTRAINT,
        SQLiteNative.RESULT_MISMATCH,
        SQLiteNative.RESULT_MISUSE,
        SQLiteNative.RESULT_NOT_IMPLEMENTED_LFS,
        SQLiteNative.RESULT_ACCESS_DENIED,
        SQLiteNative.RESULT_FORMAT,
        SQLiteNative.RESULT_RANGE,
        SQLiteNative.RESULT_NON_DB_FILE,
        SQLiteNative.RESULT_NOTICE,
        SQLiteNative.RESULT_WARNING,
        SQLiteNative.RESULT_ROW,
        SQLiteNative.RESULT_DONE})
@Retention(RetentionPolicy.SOURCE)
public @interface SQLiteResult {

}
