package com.hotwirestudios.sqlite.driver;

import org.bytedeco.javacpp.Pointer;

/**
 * Created by fmuecke on 10.11.17.
 * Needed only for sqlite3_destructor_type, which is a void *, but initialized with static int/long values casted to void *.
 */
final class StaticPointer extends Pointer {
    public StaticPointer(long address) {
        this.address = address;
    }
}
