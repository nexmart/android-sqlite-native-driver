package com.hotwirestudios.sqlite.driver;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;

/**
 * Created by FabianM on 27.08.16.
 */
public interface PrimaryKeysCallbackFunction {
    String[] call(String table);
}
