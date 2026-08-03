package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Routes parsed files to the {@link CanonicalModelParser} registered for their
 * format (strategy pattern with a Spring-injected registry).
 */
@Component
public class CanonicalParserRegistry {

    private final List<CanonicalModelParser> parsers;

    /**
     * Creates a registry from all discovered parsers.
     *
     * @param parsers all parsers registered as Spring beans
     */
    public CanonicalParserRegistry(List<CanonicalModelParser> parsers) {
        this.parsers = parsers;
    }

    /**
     * Parses the given input using the parser registered for the format.
     *
     * @param input    the file content
     * @param fileName name of the file being parsed
     * @param format   the detected format
     * @return the extracted canonical model
     * @throws IllegalArgumentException when no parser is registered for the format
     */
    public CanonicalAssetModel parse(InputStream input, String fileName, AssetFileFormat format) {
        return find(format).parse(input, fileName);
    }

    /**
     * Returns the parser registered for the given format.
     *
     * @param format the file format
     * @return the matching parser
     * @throws IllegalArgumentException when no parser handles the format
     */
    public CanonicalModelParser find(AssetFileFormat format) {
        return parsers.stream()
            .filter(p -> p.supports(format))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No parser registered for format " + format.label()));
    }
}
