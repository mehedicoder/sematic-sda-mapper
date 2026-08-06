package com.sda.mapper.hub.service;

/**
 * Thrown when a persisted Mapping Hub session cannot be restored into the
 * current session (for example persistence is unavailable or no entities were
 * found).
 */
public class SessionResumeException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message the failure description
     */
    public SessionResumeException(String message) {
        super(message);
    }
}
