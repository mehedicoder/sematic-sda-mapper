package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;

/**
 * Detects the {@link AssetFileFormat} of an uploaded file by content sniffing
 * first (to disambiguate XML/JSON variants) and by file extension as a fallback.
 */
@Component
public class HubFileTypeDetector {

    private static final int SNIFF_BYTES = 8192;

    /**
     * Detects the format, preferring content sniffing over the file extension.
     *
     * @param fileName name of the uploaded file
     * @param input    readable content of the file
     * @return the detected format, or empty when unknown
     */
    public Optional<AssetFileFormat> detect(String fileName, InputStream input) {
        Optional<AssetFileFormat> byContent = detectByContent(input);
        if (byContent.isPresent()) {
            return byContent;
        }
        return detectByFileName(fileName);
    }

    /**
     * Detects the format from the file name extension alone.
     *
     * @param fileName name of the uploaded file
     * @return the detected format, or empty for ambiguous or unknown names
     */
    public Optional<AssetFileFormat> detectByFileName(String fileName) {
        if (fileName == null) {
            return Optional.empty();
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".aml")) {
            return Optional.of(AssetFileFormat.MTP_AML);
        }
        if (lower.endsWith(".ttl") || lower.endsWith(".rdf")) {
            return Optional.of(AssetFileFormat.REQUIREMENT_TTL);
        }
        return Optional.empty();
    }

    /**
     * Detects the format by sniffing the content prefix.
     *
     * @param input readable content of the file
     * @return the detected format, or empty when the content is inconclusive
     */
    public Optional<AssetFileFormat> detectByContent(InputStream input) {
        if (input == null) {
            return Optional.empty();
        }
        try {
            byte[] prefix = input.readNBytes(SNIFF_BYTES);
            String text = new String(prefix, java.nio.charset.StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);
            return sniff(text);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Optional<AssetFileFormat> sniff(String text) {
        if (text.contains("usecaserequirement") && text.contains("requiredcapability")
                && text.contains("capabilitytype")) {
            return Optional.of(AssetFileFormat.REQUIREMENT_TTL);
        }
        if (text.contains("mtpaml") || text.contains("mtp-aml") || text.contains("opcuaserver")) {
            return Optional.of(AssetFileFormat.MTP_AML);
        }
        if (text.contains("<caex") || text.contains("caexfile") || text.contains("automationml")) {
            return Optional.of(AssetFileFormat.AUTOMATION_ML);
        }
        if (text.contains("assetadministrationshells")) {
            return Optional.of(AssetFileFormat.AAS_JSON);
        }
        if (text.contains("assetadministrationshell") && text.contains("idshort")) {
            return Optional.of(AssetFileFormat.AAS_XML);
        }
        if (text.contains("endpointdescriptions") || text.contains("nodesets")) {
            return Optional.of(AssetFileFormat.OPCUA_JSON);
        }
        if (text.contains("assetadministrationshell")) {
            return Optional.of(AssetFileFormat.AAS_XML);
        }
        return Optional.empty();
    }
}
