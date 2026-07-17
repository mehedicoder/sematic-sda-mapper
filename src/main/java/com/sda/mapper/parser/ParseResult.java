package com.sda.mapper.parser;

import org.apache.jena.rdf.model.Model;

import java.util.List;

/**
 * Immutable result of parsing an input file into SDA semantic triples.
 */
public record ParseResult(String fileType, int tripleCount, List<String> details,
                           String summary, Model model) {

    /**
     * Creates a result without a Jena Model (backward-compatible).
     */
    public ParseResult(String fileType, int tripleCount, List<String> details, String summary) {
        this(fileType, tripleCount, details, summary, null);
    }
}
