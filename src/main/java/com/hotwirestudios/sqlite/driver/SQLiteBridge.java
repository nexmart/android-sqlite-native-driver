package com.hotwirestudios.sqlite.driver;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.Platform;

/**
 * Created by FabianM on 14.06.16.
 */
@Platform(cinclude = "sqlite3.h", link = "sqlite-native-driver")
public class SQLiteBridge {
    static {
        Loader.load();
    }

    public static native int sqlite3_initialize();
}
