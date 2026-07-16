package com.sda.mapper.error;

/**
 * Unchecked exception thrown when file parsing fails due to invalid format or missing required elements.
 */
public class ParserException extends RuntimeException {

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
