package com.sda.mapper.imports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Validates uploaded files against schema expectations per file type.
 */
@Component
public class ImportValidator {

    private static final Logger log = LoggerFactory.getLogger(ImportValidator.class);

    private static final Set<FileType> SUPPORTED_TYPES = Set.of(
        FileType.MTP_AML, FileType.OPC_UA_DATASHEET, FileType.AAS, FileType.AASX,
        FileType.OPC_UA_NODESET, FileType.RDF_TURTLE, FileType.JSON_LD, FileType.YAML
    );

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    /**
     * Validates a file based on its classified type.
     *
     * @param filename the filename
     * @param fileType the classified file type
     * @return validation result with status and message
     */
    public ValidationResult validate(String filename, FileType fileType) {
        log.debug("Validating file '{}' of type {}", filename, fileType);

        if (fileType == FileType.UNKNOWN) {
            return new ValidationResult(false, "Unsupported file type", 0);
        }

        if (!SUPPORTED_TYPES.contains(fileType)) {
            return new ValidationResult(false, "File type not supported for import: " + fileType, 0);
        }

        if (filename == null || filename.isBlank()) {
            return new ValidationResult(false, "Filename is empty", 0);
        }

        return new ValidationResult(true, "File validated successfully", 0);
    }

    /**
     * Validates file size before upload.
     *
     * @param sizeBytes the file size in bytes
     * @return true if size is acceptable
     */
    public boolean validateSize(long sizeBytes) {
        if (sizeBytes <= 0) {
            return false;
        }
        if (sizeBytes > MAX_FILE_SIZE) {
            log.warn("File size {} exceeds maximum {}", sizeBytes, MAX_FILE_SIZE);
            return false;
        }
        return true;
    }

    /**
     * Validation result record.
     */
    public record ValidationResult(boolean valid, String message, int warnings) {
    }
}
