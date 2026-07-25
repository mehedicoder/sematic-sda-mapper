package com.sda.mapper.imports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Auto-classifies uploaded files by type based on file extension and content sniffing.
 */
@Component
public class FileTypeClassifier {

    private static final Logger log = LoggerFactory.getLogger(FileTypeClassifier.class);

    /**
     * Classifies a file by its filename and optional content stream.
     *
     * @param filename the original filename
     * @param input    optional input stream for content sniffing (may be null)
     * @return the detected file type
     */
    public FileType classify(String filename, InputStream input) {
        if (filename == null || filename.isBlank()) {
            return FileType.UNKNOWN;
        }

        String lower = filename.toLowerCase();
        FileType detected;

        if (lower.endsWith(".ttl") || lower.endsWith(".turtle")) {
            detected = FileType.RDF_TURTLE;
        } else if (lower.endsWith(".jsonld") || lower.endsWith(".json-ld")) {
            detected = FileType.JSON_LD;
        } else if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
            detected = FileType.YAML;
        } else if (lower.endsWith(".aasx")) {
            detected = FileType.AASX;
        } else if (lower.endsWith(".json")) {
            detected = sniffJsonType(input);
        } else if (lower.endsWith(".xml") || lower.endsWith(".aml")) {
            detected = sniffXmlType(input);
        } else {
            detected = FileType.UNKNOWN;
        }

        log.debug("Classified file '{}' as {}", filename, detected);
        return detected;
    }

    private FileType sniffJsonType(InputStream input) {
        if (input == null || !input.markSupported()) {
            return FileType.OPC_UA_DATASHEET;
        }
        try {
            byte[] buf = new byte[4096];
            input.mark(buf.length);
            int bytesRead = input.read(buf);
            input.reset();
            if (bytesRead > 0) {
                String peek = new String(buf, 0, bytesRead);
                if (peek.contains("assetAdministrationShells")) {
                    return FileType.AAS;
                }
            }
        } catch (Exception e) {
            log.debug("JSON content sniffing failed: {}", e.getMessage());
        }
        return FileType.OPC_UA_DATASHEET;
    }

    private FileType sniffXmlType(InputStream input) {
        if (input == null || !input.markSupported()) {
            return FileType.MTP_AML;
        }
        try {
            byte[] buf = new byte[4096];
            input.mark(buf.length);
            int bytesRead = input.read(buf);
            input.reset();
            if (bytesRead > 0) {
                String peek = new String(buf, 0, bytesRead);
                if (peek.contains("UANodeSet") || peek.contains("opcfoundation.org/UA")) {
                    return FileType.OPC_UA_NODESET;
                }
            }
        } catch (Exception e) {
            log.debug("XML content sniffing failed: {}", e.getMessage());
        }
        return FileType.MTP_AML;
    }
}
