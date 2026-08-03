package com.sda.mapper.hub.model;

/**
 * Describes a file that was uploaded to the mapping hub and its detected format.
 *
 * @param fileName  name of the uploaded file
 * @param format    format detected for the file
 * @param sizeBytes size of the file in bytes, or {@code 0} when unknown
 */
public record UploadedFileInfo(String fileName, AssetFileFormat format, long sizeBytes) {

    /**
     * Returns a single-line description of the uploaded file.
     *
     * @return e.g. {@code "pump-aas.json (detected: AAS JSON)"}
     */
    public String describe() {
        return fileName + " (detected: " + format.label() + ")";
    }
}
