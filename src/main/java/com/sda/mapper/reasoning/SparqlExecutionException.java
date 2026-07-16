package com.sda.mapper.reasoning;

/**
 * Thrown when a SPARQL query execution fails against the ontology model.
 */
public class SparqlExecutionException extends RuntimeException {

    public SparqlExecutionException(String message) {
        super(message);
    }

    public SparqlExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
