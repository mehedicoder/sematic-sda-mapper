package com.sda.mapper.ui;

import com.sda.mapper.parser.ParseResult;
import com.sda.mapper.parser.UploadService;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.sda.mapper.error.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * Vaadin view for uploading MTP AML and OPC UA Server Datasheet files.
 * Parses the uploaded file and displays extracted semantic triples.
 */
@Route(value = "upload", layout = MainLayout.class)
@PageTitle("Upload")
public class UploadView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(UploadView.class);

    private final UploadService uploadService;
    private final MemoryBuffer buffer;
    private final Upload upload;
    private final VerticalLayout resultsLayout;
    private final Grid<String> detailsGrid;

    public UploadView(UploadService uploadService) {
        this.uploadService = uploadService;

        setWidthFull();
        setPadding(true);
        setSpacing(true);

        add(new H3("Upload Input Files"));
        add(new Text("Upload MTP AML (.xml, .aml) or OPC UA Server Datasheet (.json) files for parsing."));

        buffer = new MemoryBuffer();
        upload = new Upload(buffer);
        upload.setWidthFull();
        upload.setAcceptedFileTypes(
            "application/xml", "text/xml",
            "application/json",
            "application/rdf+xml"
        );
        upload.setMaxFiles(1);

        upload.addSucceededListener(event -> handleUpload(event.getFileName(), buffer.getInputStream()));
        upload.addFileRemovedListener(event ->
            log.info("Upload cancelled: {}", event.getFileName()));

        add(upload);
        add(new Hr());

        resultsLayout = new VerticalLayout();
        resultsLayout.setWidthFull();
        resultsLayout.setVisible(false);
        add(resultsLayout);

        detailsGrid = new Grid<>();
        detailsGrid.addColumn(s -> s).setHeader("Details");
        detailsGrid.setWidthFull();
        detailsGrid.setHeight("300px");
    }

    private void handleUpload(String filename, InputStream inputStream) {
        try {
            ParseResult result = uploadService.parseUpload(filename, inputStream);
            showResults(filename, result);
        } catch (ParserException e) {
            log.warn("Parse failed for {}: {}", filename, e.getMessage());
            showError(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error parsing {}: {}", filename, e.getMessage(), e);
            showError("Unexpected error: " + e.getMessage());
        }
    }

    private void showResults(String filename, ParseResult result) {
        resultsLayout.removeAll();
        resultsLayout.setVisible(true);

        resultsLayout.add(new H3("Parse Results for " + filename));
        resultsLayout.add(new Text("File type: " + result.fileType()));
        resultsLayout.add(new Text("Triples generated: " + result.tripleCount()));
        resultsLayout.add(new Text(result.summary()));
        resultsLayout.add(new Hr());

        detailsGrid.setItems(result.details());
        resultsLayout.add(detailsGrid);

        Notification.show("Parsed successfully: " + result.tripleCount() + " triples",
            3000, Notification.Position.BOTTOM_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        resultsLayout.removeAll();
        resultsLayout.setVisible(true);

        resultsLayout.add(new H3("Parse Error"));
        resultsLayout.add(new Text(message));

        Notification.show("Parse failed: " + message,
            5000, Notification.Position.BOTTOM_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
