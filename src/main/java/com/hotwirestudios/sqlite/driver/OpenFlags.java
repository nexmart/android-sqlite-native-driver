package com.hotwirestudios.sqlite.driver;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by FabianM on 17.06.16.
 */
@IntDef(flag = true, value = {
        SQLiteNative.SQLITE_OPEN_READONLY,
        SQLiteNative.SQLITE_OPEN_READWRITE,
        SQLiteNative.SQLITE_OPEN_CREATE_IF_NECESSARY})
@Retention(RetentionPolicy.SOURCE)
public @interface OpenFlags {

}
