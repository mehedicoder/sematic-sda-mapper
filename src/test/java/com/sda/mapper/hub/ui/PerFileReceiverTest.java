package com.sda.mapper.hub.ui;

import com.vaadin.flow.component.upload.Receiver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies that the per-file receiver keeps distinct content for multiple
 * uploaded files, which is the bug that broke uploading two files at once with
 * a shared {@code MemoryBuffer}.
 */
@DisplayName("Per-file upload receiver")
class PerFileReceiverTest {

    @Test
    @DisplayName("Should keep distinct content for two uploaded files")
    void distinctContentsForTwoFiles() throws IOException {
        UploadDropzone.PerFileReceiver receiver = new UploadDropzone.PerFileReceiver();

        OutputStream a = receiver.receiveUpload("pump-aas.json", "application/json");
        a.write("CONTENT-A".getBytes(StandardCharsets.UTF_8));
        a.close();

        OutputStream b = receiver.receiveUpload("requirement.ttl", "text/turtle");
        b.write("CONTENT-B".getBytes(StandardCharsets.UTF_8));
        b.close();

        assertArrayEquals("CONTENT-A".getBytes(StandardCharsets.UTF_8), receiver.consume("pump-aas.json"));
        assertArrayEquals("CONTENT-B".getBytes(StandardCharsets.UTF_8), receiver.consume("requirement.ttl"));
    }

    @Test
    @DisplayName("Should return null for a file that was never received")
    void unknownFileReturnsNull() {
        UploadDropzone.PerFileReceiver receiver = new UploadDropzone.PerFileReceiver();
        assertNull(receiver.consume("missing.json"));
    }

    @Test
    @DisplayName("Should allow the same file name to be uploaded again after consumption")
    void sameNameReUploaded() throws IOException {
        UploadDropzone.PerFileReceiver receiver = new UploadDropzone.PerFileReceiver();

        OutputStream first = receiver.receiveUpload("dup.json", "application/json");
        first.write("FIRST".getBytes(StandardCharsets.UTF_8));
        first.close();
        assertArrayEquals("FIRST".getBytes(StandardCharsets.UTF_8), receiver.consume("dup.json"));

        OutputStream second = receiver.receiveUpload("dup.json", "application/json");
        second.write("SECOND".getBytes(StandardCharsets.UTF_8));
        second.close();
        assertArrayEquals("SECOND".getBytes(StandardCharsets.UTF_8), receiver.consume("dup.json"));
    }

    @Test
    @DisplayName("Receiver should provide a writable output stream")
    void providesWritableStream() {
        Receiver receiver = new UploadDropzone.PerFileReceiver();
        assertNotNull(receiver.receiveUpload("x.json", "application/json"));
    }
}
