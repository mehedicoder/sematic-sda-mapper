package com.sda.mapper.ui.importwizard;

import com.sda.mapper.imports.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.sda.mapper.ui.MainLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

@Route(value = "import/wizard", layout = MainLayout.class)
@PageTitle("Import Wizard | Semantic SDA Mapper")
public class ImportWizardView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(ImportWizardView.class);

    private final ImportSessionService sessionService;
    private final MissingInformationDetector missingInfoDetector;
    private final SemanticValidationService semanticValidationService;
    private final ImportRecommendationService recommendationService;
    private final ConflictResolutionService conflictResolutionService;
    private final SemanticCompletionService completionService;

    private UUID sessionId;
    private int currentStep = 0;

    private final VerticalLayout stepContent = new VerticalLayout();
    private final Button prevButton = new Button("Previous");
    private final Button nextButton = new Button("Next", e -> advanceStep());
    private final Button finishButton = new Button("Finish & Commit", e -> commitSession());
    private final Div stepIndicator = new Div();

    private CompletenessReport completenessReport;
    private List<MissingItem> currentMissingItems = new ArrayList<>();
    private List<MissingItem> currentResolvedItems = new ArrayList<>();

    private static final String[] STEP_LABELS = {
        "Upload Files", "Validate", "Semantic Analysis",
        "Resolve Conflicts", "Complete Missing Info", "Review & Preview", "Commit"
    };

    public ImportWizardView(ImportSessionService sessionService,
                             MissingInformationDetector missingInfoDetector,
                             SemanticValidationService semanticValidationService,
                             ImportRecommendationService recommendationService,
                             ConflictResolutionService conflictResolutionService,
                             SemanticCompletionService completionService) {
        this.sessionService = sessionService;
        this.missingInfoDetector = missingInfoDetector;
        this.semanticValidationService = semanticValidationService;
        this.recommendationService = recommendationService;
        this.conflictResolutionService = conflictResolutionService;
        this.completionService = completionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        var header = new H2("Import Wizard");
        header.addClassNames(LumoUtility.Margin.Bottom.NONE);
        add(header);

        buildStepIndicator();
        add(stepIndicator);

        stepContent.setSizeFull();
        stepContent.setPadding(false);
        add(stepContent);

        var navBar = new HorizontalLayout(prevButton, nextButton, finishButton);
        navBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        navBar.setSpacing(true);
        add(navBar);

        prevButton.addClickListener(e -> retreatStep());
        prevButton.setEnabled(false);
        finishButton.setVisible(false);

        showStep(0);
    }

    private void buildStepIndicator() {
        stepIndicator.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER,
            LumoUtility.Gap.SMALL, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.Margin.Top.SMALL);
        for (int i = 0; i < STEP_LABELS.length; i++) {
            Div dot = new Div();
            dot.addClassName("wizard-step-dot");
            dot.getElement().setAttribute("data-step", String.valueOf(i));
            dot.getElement().getStyle()
                .set("width", "12px").set("height", "12px")
                .set("border-radius", "50%")
                .set("background-color", i == 0 ? "var(--lumo-primary-color)" : "var(--lumo-contrast-20pct-color)")
                .set("transition", "background-color 0.3s");
            stepIndicator.add(dot);

            if (i < STEP_LABELS.length - 1) {
                Div line = new Div();
                line.getElement().getStyle()
                    .set("width", "40px").set("height", "2px")
                    .set("background-color", "var(--lumo-contrast-20pct-color)");
                stepIndicator.add(line);
            }
        }
    }

    private void updateStepIndicator(int step) {
        var dots = stepIndicator.getChildren()
            .filter(c -> c instanceof Div div && div.getElement().hasAttribute("data-step"))
            .toList();
        for (int i = 0; i < dots.size(); i++) {
            Div dot = (Div) dots.get(i);
            String color = i <= step ? "var(--lumo-primary-color)" : "var(--lumo-contrast-20pct-color)";
            dot.getElement().getStyle().set("background-color", color);
        }
    }

    private void showStep(int step) {
        currentStep = step;
        stepContent.removeAll();
        updateStepIndicator(step);

        prevButton.setEnabled(step > 0);
        nextButton.setVisible(step < STEP_LABELS.length - 1);
        finishButton.setVisible(step == STEP_LABELS.length - 1);

        switch (step) {
            case 0 -> showUploadStep();
            case 1 -> showValidateStep();
            case 2 -> showSemanticAnalysisStep();
            case 3 -> showConflictResolutionStep();
            case 4 -> showCompletionStep();
            case 5 -> showPreviewStep();
            case 6 -> showCommitStep();
        }
    }

    private void showUploadStep() {
        var description = new Paragraph("Upload OPC UA Datasheets, MTP AML files, AAS exports, or other supported files. " +
            "The wizard will guide you through validation, semantic analysis, and interactive completion.");
        description.addClassNames(LumoUtility.TextColor.SECONDARY);
        stepContent.add(description);

        var buffer = new MultiFileMemoryBuffer();
        var upload = new Upload(buffer);
        upload.setAcceptedFileTypes(
            ".xml", ".aml", ".json", ".aasx", ".ttl", ".turtle",
            ".jsonld", ".yaml", ".yml"
        );
        upload.setMaxFiles(20);
        upload.setWidthFull();

        var uploadInfo = new Span("Accepted: XML, AML, JSON, AASX, TTL, YAML (max 20 files)");
        uploadInfo.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        upload.addSucceededListener(event -> {
            String filename = event.getFileName();
            long size = event.getContentLength();

            if (sessionId == null) {
                var session = sessionService.createSession("Wizard Import - " + filename);
                sessionId = session.getId();
            }

            try {
                InputStream inputStream = buffer.getInputStream(filename);
                sessionService.addFile(sessionId, filename, size, inputStream);
                Notification.show("Uploaded: " + filename, 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                Notification.show("Upload failed: " + e.getMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        stepContent.add(upload, uploadInfo);
    }

    private void showValidateStep() {
        if (sessionId == null) {
            stepContent.add(new Paragraph("No files uploaded yet. Go back to Step 1."));
            nextButton.setEnabled(false);
            return;
        }
        nextButton.setEnabled(true);

        var loading = new Paragraph("Validating files...");
        loading.addClassNames(LumoUtility.TextColor.SECONDARY);
        stepContent.add(loading);

        try {
            var session = sessionService.validateSession(sessionId);
            stepContent.removeAll();

            var files = sessionService.findSession(sessionId).getFiles();

            var resultGrid = new Grid<UploadedFile>();
            resultGrid.addColumn(UploadedFile::getFilename).setHeader("File");
            resultGrid.addColumn(f -> f.getFileType().getDisplayName()).setHeader("Type");
            resultGrid.addColumn(f -> f.getStatus().name()).setHeader("Status");
            resultGrid.addColumn(UploadedFile::getValidationReport).setHeader("Report");
            resultGrid.setItems(files);
            resultGrid.setWidthFull();
            resultGrid.setHeight("300px");

            var summary = new Paragraph(String.format(
                "Validation complete: %d file(s), %d error(s), %d warning(s)",
                files.size(),
                session.getValidationErrors(),
                session.getWarnings()));
            summary.addClassNames(LumoUtility.FontWeight.BOLD);

            stepContent.add(summary, resultGrid);

            if (session.getStatus() == SessionStatus.FAILED) {
                nextButton.setEnabled(false);
                Notification.show("Session has validation errors. Fix issues or cancel.", 5000,
                    Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            stepContent.removeAll();
            stepContent.add(new Paragraph("Validation failed: " + e.getMessage()));
            nextButton.setEnabled(false);
        }
    }

    private void showSemanticAnalysisStep() {
        if (sessionId == null) {
            stepContent.add(new Paragraph("No session available. Go back to Step 1."));
            nextButton.setEnabled(false);
            return;
        }
        nextButton.setEnabled(true);

        var loading = new Paragraph("Analyzing semantic completeness...");
        loading.addClassNames(LumoUtility.TextColor.SECONDARY);
        stepContent.add(loading);

        try {
            completenessReport = sessionService.analyzeCompleteness(sessionId);
            stepContent.removeAll();

            var scoreLayout = new HorizontalLayout();
            scoreLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            scoreLayout.setSpacing(true);
            scoreLayout.setWidthFull();

            var scoreValue = new Span(String.format("%.0f%%", completenessReport.overallScore()));
            scoreValue.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);
            scoreValue.getElement().getStyle().set("color",
                completenessReport.overallScore() >= 80
                    ? "var(--lumo-success-color)"
                    : completenessReport.overallScore() >= 50
                        ? "var(--lumo-warning-color)"
                        : "var(--lumo-error-color)");

            var scoreLabel = new Span("Completeness Score");
            scoreLabel.addClassNames(LumoUtility.TextColor.SECONDARY);
            scoreLayout.add(new VerticalLayout(scoreValue, scoreLabel));

            stepContent.add(scoreLayout);

            var categoriesGrid = new Grid<Map.Entry<MissingCategory, Integer>>();
            categoriesGrid.addColumn(e -> e.getKey().getDisplayName()).setHeader("Category");
            categoriesGrid.addColumn(e -> String.valueOf(e.getValue())).setHeader("Missing Items");
            categoriesGrid.addColumn(e -> completenessReport.categoryScores().getOrDefault(e.getKey(), 0.0) + "%").setHeader("Score");
            categoriesGrid.setItems(new ArrayList<>(completenessReport.missingCounts().entrySet()));
            categoriesGrid.setWidthFull();

            stepContent.add(categoriesGrid);

            currentMissingItems = new ArrayList<>(completenessReport.missingItems());
            if (!currentMissingItems.isEmpty()) {
                var actionLabel = new Paragraph(String.format(
                    "%d items need attention. Click Next to resolve them.", currentMissingItems.size()));
                actionLabel.addClassNames(LumoUtility.FontWeight.BOLD);
                stepContent.add(actionLabel);
            } else {
                var successLabel = new Paragraph("All items are complete! No resolution needed.");
                successLabel.addClassNames(LumoUtility.TextColor.SUCCESS);
                stepContent.add(successLabel);
            }
        } catch (Exception e) {
            stepContent.removeAll();
            stepContent.add(new Paragraph("Analysis failed: " + e.getMessage()));
        }
    }

    private void showConflictResolutionStep() {
        if (currentMissingItems.isEmpty()) {
            stepContent.add(new Paragraph("No conflicts or missing items detected. Proceed to next step."));
            return;
        }

        var missingInfoPanel = new MissingInformationPanel(currentMissingItems, recommendationService);
        missingInfoPanel.setWidthFull();
        stepContent.add(missingInfoPanel);
    }

    private void showCompletionStep() {
        if (currentMissingItems.isEmpty()) {
            stepContent.add(new Paragraph("Nothing to complete. Proceed to next step."));
            return;
        }

        var form = new SemanticCompletionForm(sessionService, sessionId, currentMissingItems,
            completedItems -> {
                currentResolvedItems.addAll(completedItems);
                currentMissingItems.removeAll(completedItems);
            });
        form.setWidthFull();
        stepContent.add(form);
    }

    private void showPreviewStep() {
        var preview = new VerticalLayout();
        preview.setWidthFull();

        var title = new H3("Import Summary");
        preview.add(title);

        if (completenessReport != null) {
            var stats = new Div();
            stats.add(new Paragraph("Completeness Score: " + String.format("%.1f%%", completenessReport.overallScore())));
            stats.add(new Paragraph("Total Entities Analyzed: " + completenessReport.totalEntitiesAnalyzed()));
            stats.add(new Paragraph("Resolved Items: " + currentResolvedItems.size()));
            stats.add(new Paragraph("Remaining Missing Items: " + currentMissingItems.size()));
            preview.add(stats);
        } else {
            preview.add(new Paragraph("No analysis data available."));
        }

        stepContent.add(preview);
    }

    private void showCommitStep() {
        if (sessionId == null) {
            stepContent.add(new Paragraph("No active session. Go back to Step 1."));
            finishButton.setEnabled(false);
            return;
        }

        var loading = new Paragraph("Merging ontology data...");
        loading.addClassNames(LumoUtility.TextColor.SECONDARY);
        stepContent.add(loading);
        finishButton.setEnabled(false);

        try {
            var result = sessionService.mergeSession(sessionId);
            stepContent.removeAll();

            var icon = VaadinIcon.CHECK_CIRCLE.create();
            icon.setSize("48px");
            icon.getElement().getStyle().set("color", "var(--lumo-success-color)");

            var header = new H3("Merge Complete — Ready to Commit");
            var summary = new VerticalLayout();
            summary.setWidthFull();
            summary.add(new Paragraph("Assets found: " + result.getAssetsFound()));
            summary.add(new Paragraph("Services found: " + result.getServicesFound()));
            summary.add(new Paragraph("Conflicts detected: " + result.getConflictsDetected()));
            summary.add(new Paragraph(String.format("Completeness score: %.1f%%", result.getCompletenessScore())));
            if (result.getSummary() != null) {
                var summaryText = new Paragraph(result.getSummary());
                summaryText.addClassNames(LumoUtility.TextColor.SECONDARY);
                summary.add(summaryText);
            }

            var confirm = new Paragraph("Click 'Finish & Commit' to commit the imported ontology data.");
            confirm.addClassNames(LumoUtility.FontWeight.BOLD);

            stepContent.add(icon, header, summary, confirm);
            finishButton.setEnabled(true);
        } catch (Exception e) {
            stepContent.removeAll();
            stepContent.add(new Paragraph("Merge failed: " + e.getMessage()));
            finishButton.setEnabled(false);
        }
    }

    private void advanceStep() {
        if (currentStep < STEP_LABELS.length - 1) {
            showStep(currentStep + 1);
        }
    }

    private void retreatStep() {
        if (currentStep > 0) {
            showStep(currentStep - 1);
        }
    }

    private void commitSession() {
        if (sessionId == null) {
            Notification.show("No active session to commit.", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            sessionService.commitSession(sessionId);
            Notification.show("Import committed successfully!", 5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            sessionId = null;
            currentMissingItems.clear();
            currentResolvedItems.clear();
            completenessReport = null;
            showStep(0);
        } catch (Exception e) {
            Notification.show("Commit failed: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
