package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Reusable styled upload dropzone backed by an in-memory multi-file stream
 * handler. Files are handed to the consumer as {@code (fileName, content)} once
 * fully received. Multiple files can be selected at once: the picker is a
 * multi-select because the upload uses the server-side {@link UploadHandler}
 * stream API rather than the legacy single-file {@code Receiver}.
 */
public class UploadDropzone {

    private final Upload upload;

    /**
     * Creates a dropzone.
     *
     * @param dropLabel       drop-area label, e.g. {@code "Drop OPC UA JSON here"}
     * @param acceptedFormats formats accepted by the picker, or {@code null} to accept anything
     * @param onFileReceived  callback with the file name and content
     * @param onError         callback for rejected or failed uploads
     */
    public UploadDropzone(String dropLabel, List<AssetFileFormat> acceptedFormats,
                          BiConsumer<String, byte[]> onFileReceived,
                          Consumer<String> onError) {
        upload = new Upload(UploadHandler.inMemory(
                (metadata, bytes) -> onFileReceived.accept(metadata.fileName(), bytes)));
        upload.setDropLabel(new Div(new Text(dropLabel)));
        upload.setUploadButton(new Button("Browse files"));
        upload.addClassName("hub-dropzone");
        if (acceptedFormats != null && !acceptedFormats.isEmpty()) {
            upload.setAcceptedFileExtensions(acceptedFormats.stream()
                    .flatMap(f -> extensions(f).stream())
                    .toArray(String[]::new));
        }
        upload.setMaxFiles(50);
        upload.addFileRejectedListener(event -> onError.accept(
                "File rejected: " + event.getErrorMessage()));
    }

    /**
     * Returns the underlying Vaadin upload component.
     *
     * @return the upload component
     */
    public Upload get() {
        return upload;
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
