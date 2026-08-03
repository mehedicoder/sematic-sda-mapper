package com.sda.mapper.hub.parser;

import java.util.Optional;

/**
 * Unchecked exception raised when an asset description file cannot be parsed,
 * carrying the offending file name and, when known, the line and column.
 */
public class ParsingException extends RuntimeException {

    private final String fileName;
    private final Integer line;
    private final Integer column;

    /**
     * Creates a parsing exception with a message.
     *
     * @param fileName name of the offending file
     * @param message  the error message
     */
    public ParsingException(String fileName, String message) {
        this(fileName, message, null, null, null);
    }

    /**
     * Creates a parsing exception with a message and cause.
     *
     * @param fileName name of the offending file
     * @param message  the error message
     * @param cause    the underlying cause
     */
    public ParsingException(String fileName, String message, Throwable cause) {
        this(fileName, message, cause, null, null);
    }

    /**
     * Creates a parsing exception with location hints.
     *
     * @param fileName name of the offending file
     * @param message  the error message
     * @param line     the offending line, or {@code null} when unknown
     * @param column   the offending column, or {@code null} when unknown
     */
    public ParsingException(String fileName, String message, Integer line, Integer column) {
        this(fileName, message, null, line, column);
    }

    private ParsingException(String fileName, String message, Throwable cause, Integer line, Integer column) {
        super(buildMessage(fileName, message, line, column), cause);
        this.fileName = fileName;
        this.line = line;
        this.column = column;
    }

    /**
     * Returns the name of the offending file.
     *
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the offending line when known.
     *
     * @return the line number, or empty
     */
    public Optional<Integer> getLine() {
        return Optional.ofNullable(line);
    }

    /**
     * Returns the offending column when known.
     *
     * @return the column number, or empty
     */
    public Optional<Integer> getColumn() {
        return Optional.ofNullable(column);
    }

    private static String buildMessage(String fileName, String message, Integer line, Integer column) {
        StringBuilder builder = new StringBuilder("Failed to parse '")
            .append(fileName).append("': ").append(message);
        if (line != null) {
            builder.append(" (line ").append(line);
            if (column != null) {
                builder.append(", column ").append(column);
            }
            builder.append(')');
        }
        return builder.toString();
    }
}
