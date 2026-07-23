package com.sda.mapper.error;

/**
 * Thrown when a service registration attempt conflicts with an existing ontology individual
 * (same label and type already present).
 */
public class OntologyConflictException extends RuntimeException {

    public OntologyConflictException(String message) {
        super(message);
    }

    public OntologyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
