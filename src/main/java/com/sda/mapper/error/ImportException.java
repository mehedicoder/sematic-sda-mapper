package com.sda.mapper.error;

/**
 * Unchecked exception thrown when import session operations fail.
 */
public class ImportException extends RuntimeException {

    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
