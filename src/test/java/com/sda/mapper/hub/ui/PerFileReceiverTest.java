package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.vaadin.flow.component.upload.Upload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the dropzone enables multiple file selection through the
 * modern multi-file stream handler and accepts every supported input format.
 */
@DisplayName("Multi-file upload dropzone")
class PerFileReceiverTest {

    private UploadDropzone createDropzone() {
        return new UploadDropzone("drop", List.of(AssetFileFormat.values()),
                (name, content) -> {
                }, msg -> {
        });
    }

    @Test
    @DisplayName("Should allow selecting more than one file at a time")
    void allowsMultipleFiles() {
        Upload upload = createDropzone().get();
        assertTrue(upload.getMaxFiles() > 1,
                "upload must be configured for multiple file selection");
    }

    @Test
    @DisplayName("Should configure accepted file extensions for the picker")
    void configuresAcceptedExtensions() {
        UploadDropzone dropzone = createDropzone();
        List<String> accepted = dropzone.get().getAcceptedFileExtensions();
        assertFalse(accepted.isEmpty(), "expected at least one accepted extension");
        assertTrue(accepted.contains(".aml"), "expected MTP/AutomationML .aml to be accepted");
        assertTrue(accepted.contains(".xml"), "expected AAS/OPC-UA XML to be accepted");
        assertTrue(accepted.contains(".json"), "expected AAS/OPC-UA JSON to be accepted");
        assertTrue(accepted.contains(".ttl"), "expected requirement/ABox .ttl to be accepted");
    }

    @Test
    @DisplayName("Every supported format contributes its extensions to the configured set")
    void allFormatsAccepted() {
        List<String> accepted = createDropzone().get().getAcceptedFileExtensions();
        for (AssetFileFormat format : AssetFileFormat.values()) {
            for (String ext : extensions(format)) {
                assertTrue(accepted.contains(ext), "expected extension " + ext + " for " + format);
            }
        }
    }

    private static List<String> extensions(AssetFileFormat format) {
        return switch (format) {
            case AAS_XML -> List.of(".xml");
            case AAS_JSON -> List.of(".json");
            case MTP_AML -> List.of(".aml", ".xml");
            case AUTOMATION_ML -> List.of(".aml", ".xml");
            case OPCUA_JSON -> List.of(".json");
            case OPCUA_XML -> List.of(".aml", ".xml");
            case REQUIREMENT_TTL -> List.of(".ttl", ".rdf");
            case ABOX_TTL -> List.of(".ttl", ".rdf");
        };
    }
}
