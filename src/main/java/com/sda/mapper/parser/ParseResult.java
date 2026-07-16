package com.sda.mapper.parser;

import java.util.List;

/**
 * Immutable result of parsing an input file into SDA semantic triples.
 */
public record ParseResult(String fileType, int tripleCount, List<String> details, String summary) {
}
