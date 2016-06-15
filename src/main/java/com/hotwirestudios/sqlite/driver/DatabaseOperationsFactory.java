package com.hotwirestudios.sqlite.driver;

import android.support.annotation.Nullable;

/**
 * Created by FabianM on 18.05.16.
 */
public interface DatabaseOperationsFactory<TOperations> {
    TOperations createOperations(@Nullable SQLiteConnection connection);
}
