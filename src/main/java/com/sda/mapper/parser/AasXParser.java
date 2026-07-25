package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Parses AASX (Asset Administration Shell Exchange) package files.
 * An AASX file is a ZIP archive containing {@code data.json} (AAS JSON flat format)
 * plus OPC package metadata. This parser extracts {@code data.json} and delegates
 * to {@link AasParser} for actual RDF triple generation.
 */
@Component
public class AasXParser implements Parser {

    private static final Logger log = LoggerFactory.getLogger(AasXParser.class);

    private static final String[] DATA_JSON_CANDIDATES = {
        "data.json",
        "aasx/data.json",
        "Data.json",
        "AASX/data.json"
    };

    private final AasParser aasParser;

    public AasXParser(AasParser aasParser) {
        this.aasParser = aasParser;
    }

    @Override
    public ParseResult parse(InputStream input) {
        if (input == null) {
            throw new ParserException("Input stream must not be null");
        }

        byte[] dataJsonBytes = extractDataJson(input);
        if (dataJsonBytes == null) {
            throw new ParserException(
                "AASX package does not contain data.json. "
                + "Expected one of: data.json, aasx/data.json");
        }

        if (dataJsonBytes.length == 0) {
            throw new ParserException("AASX data.json is empty");
        }

        try (InputStream dataJsonStream = new ByteArrayInputStream(dataJsonBytes)) {
            ParseResult result = aasParser.parse(dataJsonStream);
            return new ParseResult(
                "AASX",
                result.tripleCount(),
                result.details(),
                result.summary(),
                result.model()
            );
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Failed to parse AASX data.json: " + e.getMessage(), e);
        }
    }

    private byte[] extractDataJson(InputStream input) {
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (isDataJsonEntry(entryName)) {
                    log.debug("Found data.json in AASX at: {}", entryName);
                    return readAllBytes(zip);
                }
            }
        } catch (java.util.zip.ZipException e) {
            throw new ParserException("Invalid AASX file: not a valid ZIP archive", e);
        } catch (IOException e) {
            throw new ParserException("Failed to read AASX package: " + e.getMessage(), e);
        }
        return null;
    }

    private boolean isDataJsonEntry(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return false;
        }
        String normalized = entryName.replace('\\', '/');
        for (String candidate : DATA_JSON_CANDIDATES) {
            if (normalized.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
}
