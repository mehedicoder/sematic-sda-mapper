package com.sda.mapper.parser;

import java.io.InputStream;

/**
 * Strategy interface for parsing input files into SDA semantic triples.
 */
public interface Parser {

    /**
     * Parses the given input stream and returns a result describing the extracted triples.
     *
     * @param input the file content to parse
     * @return parse result with triple count and details
     * @throws com.sda.mapper.error.ParserException if the input is invalid or missing required elements
     */
    ParseResult parse(InputStream input);
}
