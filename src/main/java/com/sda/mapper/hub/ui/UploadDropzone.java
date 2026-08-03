package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Reusable styled upload dropzone backed by an in-memory receiver. Files are
 * handed to the consumer as {@code (fileName, content)} once fully received.
 */
public class UploadDropzone {

    private final MemoryBuffer buffer = new MemoryBuffer();
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
        upload = new Upload(buffer);
        upload.setDropLabel(new Div(new Text(dropLabel)));
        upload.setUploadButton(new Button("Browse files"));
        upload.addClassName("hub-dropzone");
        if (acceptedFormats != null && !acceptedFormats.isEmpty()) {
            upload.setAcceptedFileTypes(acceptedFormats.stream()
                .flatMap(f -> mimeTypes(f).stream())
                .toArray(String[]::new));
        }
        upload.addSucceededListener(event -> {
            try {
                String fileName = event.getFileName();
                byte[] content = buffer.getInputStream().readAllBytes();
                onFileReceived.accept(fileName, content);
            } catch (IOException e) {
                onError.accept("Could not read uploaded file: " + e.getMessage());
            }
        });
        upload.addFailedListener(event -> onError.accept(
            "Upload failed: " + event.getReason()));
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

    private static List<String> mimeTypes(AssetFileFormat format) {
        return switch (format) {
            case AAS_XML -> List.of("application/xml", ".xml");
            case AAS_JSON -> List.of("application/json", ".json");
            case MTP_AML -> List.of("application/xml", ".xml", ".aml");
            case OPCUA_JSON -> List.of("application/json", ".json");
        };
    }
}
