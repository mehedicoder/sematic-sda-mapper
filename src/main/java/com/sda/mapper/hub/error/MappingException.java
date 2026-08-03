package com.sda.mapper.hub.error;

/**
 * Raised when a mapping hub operation cannot produce a result, e.g. when the
 * manifest export fails to serialize.
 */
public class MappingException extends RuntimeException {

    /**
     * Creates a mapping exception with a message.
     *
     * @param message the error message
     */
    public MappingException(String message) {
        super(message);
    }

    /**
     * Creates a mapping exception with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
