package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.Upload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Reusable styled upload dropzone backed by an in-memory receiver. Files are
 * handed to the consumer as {@code (fileName, content)} once fully received.
 */
public class UploadDropzone {

    private final PerFileReceiver receiver = new PerFileReceiver();
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
        upload = new Upload(receiver);
        upload.setDropLabel(new Div(new Text(dropLabel)));
        upload.setUploadButton(new Button("Browse files"));
        upload.addClassName("hub-dropzone");
        if (acceptedFormats != null && !acceptedFormats.isEmpty()) {
            upload.setAcceptedFileTypes(acceptedFormats.stream()
                .flatMap(f -> mimeTypes(f).stream())
                .toArray(String[]::new));
        }
        upload.addSucceededListener(event -> {
            byte[] content = receiver.consume(event.getFileName());
            if (content == null) {
                onError.accept("Could not read uploaded file: " + event.getFileName());
                return;
            }
            onFileReceived.accept(event.getFileName(), content);
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
            case AUTOMATION_ML -> List.of("application/xml", ".xml", ".aml");
            case OPCUA_JSON -> List.of("application/json", ".json");
            case OPCUA_XML -> List.of("application/xml", ".xml", ".aml");
            case REQUIREMENT_TTL -> List.of("text/turtle", ".ttl", ".rdf");
            case ABOX_TTL -> List.of("text/turtle", ".ttl", ".rdf");
        };
    }

    /**
     * Per-file in-memory receiver. Unlike {@code MemoryBuffer}, which keeps a
     * single shared buffer that the next upload overwrites, this keeps a
     * separate stream per file name so multiple dropped files each retain their
     * own content. The buffer is removed once consumed to avoid name collisions
     * when a file with the same name is uploaded again.
     */
    static class PerFileReceiver implements Receiver {

        private final Map<String, ByteArrayOutputStream> buffers = new ConcurrentHashMap<>();

        @Override
        public OutputStream receiveUpload(String fileName, String mimeType) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            buffers.put(fileName, out);
            return out;
        }

        byte[] consume(String fileName) {
            ByteArrayOutputStream out = buffers.remove(fileName);
            return out == null ? null : out.toByteArray();
        }
    }
}
