package com.sda.mapper.hub.model;

import java.util.List;

/**
 * A context-aware recommendation of a supplementary upload for the mapping hub.
 *
 * @param format the recommended file format
 * @param reason human-readable reason why this format helps
 * @param fills  ontology classes this upload would cover that are currently open
 */
public record Recommendation(AssetFileFormat format, String reason, List<String> fills) {
}
