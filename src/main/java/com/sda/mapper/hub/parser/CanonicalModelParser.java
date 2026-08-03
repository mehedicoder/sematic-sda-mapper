package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;

import java.io.InputStream;

/**
 * Strategy contract for parsing an asset description format into the canonical
 * asset model of the mapping hub.
 */
public interface CanonicalModelParser {

    /**
     * Returns the file format this parser handles.
     *
     * @return the handled format
     */
    AssetFileFormat format();

    /**
     * Parses the given input into canonical entities.
     *
     * @param input    the file content
     * @param fileName name of the file being parsed, used for provenance
     * @return the extracted canonical model (never {@code null})
     * @throws ParsingException when the content is invalid or not this format
     */
    CanonicalAssetModel parse(InputStream input, String fileName);

    /**
     * Returns whether this parser handles the given format.
     *
     * @param format the candidate format
     * @return {@code true} when this parser handles the format
     */
    default boolean supports(AssetFileFormat format) {
        return format() == format;
    }
}
