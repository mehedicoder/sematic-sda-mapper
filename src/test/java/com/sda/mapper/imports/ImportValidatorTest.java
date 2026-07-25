package com.sda.mapper.imports;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Import Validator")
class ImportValidatorTest {

    private final ImportValidator validator = new ImportValidator();

    @Test
    @DisplayName("Should validate MTP_AML files as valid")
    void shouldValidateMtpAmlFiles() {
        var result = validator.validate("service.xml", FileType.MTP_AML);
        assertTrue(result.valid());
    }

    @Test
    @DisplayName("Should validate OPC_UA_DATASHEET files as valid")
    void shouldValidateOpcUaDatasheetFiles() {
        var result = validator.validate("datasheet.json", FileType.OPC_UA_DATASHEET);
        assertTrue(result.valid());
    }

    @Test
    @DisplayName("Should validate AAS files as valid")
    void shouldValidateAasFiles() {
        var result = validator.validate("aas.json", FileType.AAS);
        assertTrue(result.valid());
    }

    @Test
    @DisplayName("Should validate OPC_UA_NODESET files as valid")
    void shouldValidateNodeSetFiles() {
        var result = validator.validate("nodeset.xml", FileType.OPC_UA_NODESET);
        assertTrue(result.valid());
    }

    @Test
    @DisplayName("Should validate RDF_TURTLE files as valid")
    void shouldValidateTurtleFiles() {
        var result = validator.validate("ontology.ttl", FileType.RDF_TURTLE);
        assertTrue(result.valid());
    }

    @Test
    @DisplayName("Should reject UNKNOWN file type")
    void shouldRejectUnknownFileType() {
        var result = validator.validate("readme.txt", FileType.UNKNOWN);
        assertFalse(result.valid());
        assertTrue(result.message().contains("Unsupported"));
    }

    @Test
    @DisplayName("Should reject blank filename")
    void shouldRejectBlankFilename() {
        var result = validator.validate("", FileType.MTP_AML);
        assertFalse(result.valid());
        assertTrue(result.message().contains("empty"));
    }

    @Test
    @DisplayName("Should accept valid file size")
    void shouldAcceptValidFileSize() {
        assertTrue(validator.validateSize(1024));
        assertTrue(validator.validateSize(1024 * 1024));
    }

    @Test
    @DisplayName("Should reject zero or negative file size")
    void shouldRejectInvalidFileSize() {
        assertFalse(validator.validateSize(0));
        assertFalse(validator.validateSize(-1));
    }

    @Test
    @DisplayName("Should reject oversized files")
    void shouldRejectOversizedFiles() {
        assertFalse(validator.validateSize(100L * 1024 * 1024));
    }
}
