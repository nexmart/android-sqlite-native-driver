package com.hotwirestudios.sqlite.driver;

import org.bytedeco.javacpp.PointerPointer;

/**
 * Created by FabianM on 17.06.16.
 */
public interface SQLiteFunction {
    void call(SQLiteNative.ContextHandle context, int argc, PointerPointer<SQLiteNative.ValueHandle> argv);
}
