package com.hotwirestudios.sqlite.driver;

/**
 * Allows throwing SQLiteExceptions if a statement does not perform as expected.
 */
public interface SQLiteResultHandler {
    /**
     * Throws a SQLiteException if the provided code does not match the expected.
     *
     * @param code     The actual result code
     * @param expected The expected result code
     * @throws SQLiteException
     */
    void handleResultCode(@SQLiteResult int code, @SQLiteResult int expected) throws SQLiteException;

    /**
     * Throws a SQLiteException with a custom result code.
     *
     * @param code The result code
     * @throws SQLiteException
     */
    void throwExceptionWithCode(@SQLiteResult int code) throws SQLiteException;
}
