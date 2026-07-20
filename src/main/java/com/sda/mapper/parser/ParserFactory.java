package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory that selects the appropriate {@link Parser} implementation based on file extension
 * and XML content inspection.
 */
@Component
public class ParserFactory {

    private static final Logger log = LoggerFactory.getLogger(ParserFactory.class);

    private final MTPAmlParser mtpAmlParser;
    private final UaServerDatasheetParser uaServerDatasheetParser;
    private final OpcUaNodeSetParser opcUaNodeSetParser;
    private final AasParser aasParser;

    public ParserFactory(MTPAmlParser mtpAmlParser, UaServerDatasheetParser uaServerDatasheetParser,
                         OpcUaNodeSetParser opcUaNodeSetParser, AasParser aasParser) {
        this.mtpAmlParser = mtpAmlParser;
        this.uaServerDatasheetParser = uaServerDatasheetParser;
        this.opcUaNodeSetParser = opcUaNodeSetParser;
        this.aasParser = aasParser;
    }

    /**
     * Returns the parser appropriate for the given filename and content.
     * For XML files, inspects the root element to distinguish OPC UA NodeSet
     * from MTP AML format.
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
        if (lower.endsWith(".json")) {
            return uaServerDatasheetParser;
        } else if (lower.endsWith(".xml") || lower.endsWith(".aml")) {
            return mtpAmlParser;
        } else {
            throw new ParserException("Unsupported file extension for: " + filename
                + ". Supported extensions: .xml, .aml, .json");
        }
    }

    /**
     * Returns the parser appropriate for the given filename and input content.
     * For JSON files, inspects the root element to distinguish AAS from OPC UA
     * Datasheet format. For XML files, inspects the root element to distinguish
     * OPC UA NodeSet from MTP AML format.
     *
     * @param filename the original filename of the uploaded file
     * @param input    the file content stream (must support mark/reset)
     * @return the matching parser
     * @throws ParserException if the file extension is not supported
     */
    public Parser getParser(String filename, java.io.InputStream input) throws ParserException {
        if (filename == null || filename.isBlank()) {
            throw new ParserException("Filename must not be null or blank");
        }

        String lower = filename.toLowerCase();
        if (lower.endsWith(".json")) {
            if (input != null && input.markSupported()) {
                return sniffJsonParser(input, uaServerDatasheetParser);
            }
            return uaServerDatasheetParser;
        } else if (lower.endsWith(".xml") || lower.endsWith(".aml")) {
            if (input != null && input.markSupported()) {
                return sniffXmlParser(input, mtpAmlParser);
            }
            return mtpAmlParser;
        } else {
            throw new ParserException("Unsupported file extension for: " + filename
                + ". Supported extensions: .xml, .aml, .json");
        }
    }

    private Parser sniffJsonParser(java.io.InputStream input, Parser fallback) {
        try {
            byte[] buf = new byte[4096];
            input.mark(buf.length);
            int bytesRead = input.read(buf);
            input.reset();
            if (bytesRead > 0) {
                String peek = new String(buf, 0, bytesRead);
                if (peek.contains("assetAdministrationShells")) {
                    log.info("Detected AAS JSON via content sniffing");
                    return aasParser;
                }
            }
        } catch (Exception e) {
            log.debug("JSON content sniffing failed, falling back to default parser: {}",
                e.getMessage());
        }
        return fallback;
    }

    private Parser sniffXmlParser(java.io.InputStream input, Parser fallback) {
        try {
            byte[] buf = new byte[4096];
            input.mark(buf.length);
            int bytesRead = input.read(buf);
            input.reset();
            if (bytesRead > 0) {
                String peek = new String(buf, 0, bytesRead);
                if (peek.contains("UANodeSet") || peek.contains("opcfoundation.org/UA")) {
                    log.info("Detected OPC UA NodeSet XML via content sniffing");
                    return opcUaNodeSetParser;
                }
            }
        } catch (Exception e) {
            log.debug("Content sniffing failed, falling back to default parser: {}", e.getMessage());
        }
        return fallback;
    }
}
