package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
import org.springframework.stereotype.Component;

/**
 * Factory that selects the appropriate {@link Parser} implementation based on file extension.
 */
@Component
public class ParserFactory {

    private final MTPAmlParser mtpAmlParser;
    private final UaServerDatasheetParser uaServerDatasheetParser;

    public ParserFactory(MTPAmlParser mtpAmlParser, UaServerDatasheetParser uaServerDatasheetParser) {
        this.mtpAmlParser = mtpAmlParser;
        this.uaServerDatasheetParser = uaServerDatasheetParser;
    }

    /**
     * Returns the parser appropriate for the given filename.
     *
     * @param filename the original filename of the uploaded file
     * @return the matching parser
     * @throws ParserException if the file extension is not supported
     */
    public Parser getParser(String filename) throws ParserException {
        if (filename == null || filename.isBlank()) {
            throw new ParserException("Filename must not be null or blank");
        }

        String lower = filename.toLowerCase();
        if (lower.endsWith(".xml") || lower.endsWith(".aml")) {
            return mtpAmlParser;
        } else if (lower.endsWith(".json")) {
            return uaServerDatasheetParser;
        } else {
            throw new ParserException("Unsupported file extension for: " + filename
                + ". Supported extensions: .xml, .aml, .json");
        }
    }
}
