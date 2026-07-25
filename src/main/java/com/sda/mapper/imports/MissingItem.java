package com.sda.mapper.imports;

/**
 * A single piece of missing semantic information detected during import analysis.
 */
public record MissingItem(
    MissingCategory category,
    String entityUri,
    String entityLabel,
    String entityType,
    String fieldPath,
    String description,
    boolean resolvable
) {
}
