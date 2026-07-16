package com.sda.mapper.error;

/**
 * Thrown when the CSS-SDA ontology file cannot be loaded, parsed, or is missing.
 */
public class OntologyLoadingException extends RuntimeException {

    public OntologyLoadingException(String message) {
        super(message);
    }

    public OntologyLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
