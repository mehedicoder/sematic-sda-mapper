package com.sda.mapper.ui;

import com.sda.mapper.imports.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Vaadin view for multi-file import workspace.
 * Provides drag-and-drop upload zone, file list with status,
 * validation panel, conflict display, and merge/commit actions.
 */
@Route(value = "import", layout = MainLayout.class)
@PageTitle("Import Workspace | Semantic SDA Mapper")
public class ImportWorkspaceView extends VerticalLayout {

    private final ImportSessionService importSessionService;

    private ImportSession currentSession;
    private final List<UploadedFileInfo> uploadedFiles = new ArrayList<>();

    private final Grid<UploadedFileInfo> fileGrid = new Grid<>();
    private final Paragraph statusLabel = new Paragraph();
    private final Button validateButton = new Button("Validate");
    private final Button mergeButton = new Button("Merge");
    private final Button commitButton = new Button("Commit");
    private final Button cancelButton = new Button("Cancel Session");
    private final VerticalLayout summaryPanel = new VerticalLayout();

    public ImportWorkspaceView(ImportSessionService importSessionService) {
        this.importSessionService = importSessionService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Multi-File Import Workspace"));
        add(new Paragraph("Upload multiple heterogeneous device description files (MTP AML, OPC UA Datasheet, AAS, AASX, NodeSet, RDF/Turtle, JSON-LD, YAML) for semantic import."));

        createUploadSection();
        createFileGrid();
        createActionButtons();
        createSummaryPanel();
    }

    private void createUploadSection() {
        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(
            ".xml", ".aml", ".json", ".aasx", ".ttl", ".turtle", ".jsonld", ".yaml", ".yml");
        upload.setMaxFiles(20);
        upload.setDropLabel(new Paragraph("Drag and drop files here or click to browse"));
        upload.setWidthFull();

        upload.addSucceededListener(event -> {
            String filename = event.getMIMEType();
            InputStream inputStream = buffer.getInputStream(event.getFileName());

            if (currentSession == null) {
                currentSession = importSessionService.createSession("Import Session");
            }

            try {
                UploadedFile file = importSessionService.addFile(
                    currentSession.getId(), event.getFileName(), 0, inputStream);
                uploadedFiles.add(new UploadedFileInfo(
                    file.getId(), event.getFileName(), file.getFileType().name(),
                    "UPLOADED", 0, 0));
                fileGrid.setItems(uploadedFiles);
                updateStatus("File uploaded: " + event.getFileName());
            } catch (Exception e) {
                Notification.show("Failed to upload file: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE);
            }
        });

        add(upload);
    }

    private void createFileGrid() {
        fileGrid.addColumn(UploadedFileInfo::filename).setHeader("Filename");
        fileGrid.addColumn(UploadedFileInfo::fileType).setHeader("Type");
        fileGrid.addColumn(UploadedFileInfo::status).setHeader("Status");
        fileGrid.addColumn(f -> formatSize(f.sizeBytes())).setHeader("Size");
        fileGrid.addColumn(UploadedFileInfo::tripleCount).setHeader("Triples");
        fileGrid.setWidthFull();
        fileGrid.setHeight("300px");
        add(fileGrid);
    }

    private void createActionButtons() {
        validateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        validateButton.setEnabled(false);
        validateButton.addClickListener(e -> validateSession());

        mergeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        mergeButton.setEnabled(false);
        mergeButton.addClickListener(e -> mergeSession());

        commitButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        commitButton.setEnabled(false);
        commitButton.addClickListener(e -> commitSession());

        cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancelButton.setEnabled(false);
        cancelButton.addClickListener(e -> cancelSession());

        HorizontalLayout actions = new HorizontalLayout(
            validateButton, mergeButton, commitButton, cancelButton);
        actions.setSpacing(true);
        add(actions);

        add(statusLabel);
    }

    private void createSummaryPanel() {
        summaryPanel.setWidthFull();
        summaryPanel.setVisible(false);
        add(summaryPanel);
    }

    private void validateSession() {
        if (currentSession == null) return;

        try {
            currentSession = importSessionService.validateSession(currentSession.getId());
            updateStatus("Validation complete. Errors: " + currentSession.getValidationErrors()
                + ", Warnings: " + currentSession.getWarnings());
            mergeButton.setEnabled(currentSession.getValidationErrors() == 0);
        } catch (Exception e) {
            Notification.show("Validation failed: " + e.getMessage(),
                5000, Notification.Position.MIDDLE);
        }
    }

    private void mergeSession() {
        if (currentSession == null) return;

        try {
            ImportResult result = importSessionService.mergeSession(currentSession.getId());
            currentSession = importSessionService.findSession(currentSession.getId());
            updateStatus("Merge complete. Assets: " + result.getAssetsFound()
                + ", Services: " + result.getServicesFound()
                + ", Conflicts: " + result.getConflictsDetected());
            commitButton.setEnabled(true);
            showSummary(result);
        } catch (Exception e) {
            Notification.show("Merge failed: " + e.getMessage(),
                5000, Notification.Position.MIDDLE);
        }
    }

    private void commitSession() {
        if (currentSession == null) return;

        try {
            currentSession = importSessionService.commitSession(currentSession.getId());
            updateStatus("Session committed successfully!");
            Notification.show("Import committed to ontology",
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Commit failed: " + e.getMessage(),
                5000, Notification.Position.MIDDLE);
        }
    }

    private void cancelSession() {
        if (currentSession != null) {
            importSessionService.deleteSession(currentSession.getId());
            currentSession = null;
            uploadedFiles.clear();
            fileGrid.setItems(uploadedFiles);
            validateButton.setEnabled(false);
            mergeButton.setEnabled(false);
            commitButton.setEnabled(false);
            cancelButton.setEnabled(false);
            summaryPanel.setVisible(false);
            updateStatus("Session cancelled.");
        }
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        cancelButton.setEnabled(currentSession != null);
        boolean hasFiles = !uploadedFiles.isEmpty();
        validateButton.setEnabled(hasFiles && currentSession != null);
    }

    private void showSummary(ImportResult result) {
        summaryPanel.setVisible(true);
        summaryPanel.removeAll();

        summaryPanel.add(new H3("Import Summary"));
        summaryPanel.add(new Paragraph("Assets: " + result.getAssetsFound()));
        summaryPanel.add(new Paragraph("Services: " + result.getServicesFound()));
        summaryPanel.add(new Paragraph("Capabilities: " + result.getCapabilitiesFound()));
        summaryPanel.add(new Paragraph("Skills: " + result.getSkillsFound()));
        summaryPanel.add(new Paragraph("Communication Bindings: " + result.getCommunicationBindingsFound()));
        summaryPanel.add(new Paragraph("Data Connectors: " + result.getDataConnectorsFound()));
        summaryPanel.add(new Paragraph("Conflicts: " + result.getConflictsDetected()));
        summaryPanel.add(new Paragraph("Completeness Score: " + String.format("%.1f%%", result.getCompletenessScore())));
        if (result.getSummary() != null) {
            summaryPanel.add(new Paragraph(result.getSummary()));
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private record UploadedFileInfo(
        java.util.UUID id,
        String filename,
        String fileType,
        String status,
        long sizeBytes,
        int tripleCount
    ) {
    }
}
