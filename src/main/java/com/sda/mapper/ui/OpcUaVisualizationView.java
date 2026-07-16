package com.sda.mapper.ui;

import com.sda.mapper.opcua.OpcUaSnapshot;
import com.sda.mapper.opcua.OpcUaVisualizationService;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.Timer;
import java.util.TimerTask;

@Route(value = "opcua-visualization", layout = MainLayout.class)
@PageTitle("OPC UA Monitor")
public class OpcUaVisualizationView extends VerticalLayout {

    private final OpcUaVisualizationService visualizationService;

    private Span connectionStatus;
    private Span tankLevelValue;
    private Span flowInValue;
    private Span flowOutValue;
    private Span temperatureValue;
    private Span pumpFlowValue;
    private Span pumpVibrationValue;
    private Span pumpHealthStatus;
    private Span ne107Badge;

    private Timer refreshTimer;

    public OpcUaVisualizationView(OpcUaVisualizationService visualizationService) {
        this.visualizationService = visualizationService;
        buildLayout();
        startPolling();
    }

    private void buildLayout() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        var header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setWidthFull();

        var title = new H2("OPC UA Monitor");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE);

        connectionStatus = new Span("Disconnected");
        connectionStatus.addClassNames(
            LumoUtility.FontSize.SMALL,
            LumoUtility.BorderRadius.SMALL,
            LumoUtility.Padding.SMALL);
        connectionStatus.getStyle()
            .set("background", "var(--lumo-error-color)")
            .set("color", "white")
            .set("border-radius", "4px");

        header.add(title, connectionStatus);
        header.setFlexGrow(1, title);
        add(header);

        add(createTankSection());
        add(createPumpSection());
    }

    private VerticalLayout createTankSection() {
        var section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(false);
        section.setWidthFull();
        section.addClassName("monitoring-card");
        section.getStyle()
            .set("border-radius", "12px")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)");

        var header = new HorizontalLayout(new Icon(VaadinIcon.CUBE), new Span("Tank"));
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        section.add(header);

        var levelRow = createMetricRow("Level (0-100 cm)");
        tankLevelValue = (Span) levelRow.getChildren()
            .filter(c -> c instanceof Span s && s != levelRow.getChildren().findFirst().orElse(null))
            .findFirst().orElse(new Span("—"));
        section.add(levelRow);

        var flowInRow = createMetricRow("Flow Rate In (L/min)");
        flowInValue = getMetricSpan(flowInRow);
        section.add(flowInRow);

        var flowOutRow = createMetricRow("Flow Rate Out (L/min)");
        flowOutValue = getMetricSpan(flowOutRow);
        section.add(flowOutRow);

        var tempRow = createMetricRow("Temperature (°C)");
        temperatureValue = getMetricSpan(tempRow);
        section.add(tempRow);

        return section;
    }

    private VerticalLayout createPumpSection() {
        var section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(false);
        section.setWidthFull();
        section.addClassName("monitoring-card");
        section.getStyle()
            .set("border-radius", "12px")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)");

        var header = new HorizontalLayout(new Icon(VaadinIcon.COG), new Span("Pump"));
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        section.add(header);

        var flowRow = createMetricRow("Flow Rate (L/min)");
        pumpFlowValue = getMetricSpan(flowRow);
        section.add(flowRow);

        var vibRow = createMetricRow("Vibration (mm/s)");
        pumpVibrationValue = getMetricSpan(vibRow);
        section.add(vibRow);

        var healthLabel = new Span("Health Status");
        healthLabel.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        pumpHealthStatus = new Span("—");
        pumpHealthStatus.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        section.add(healthLabel, pumpHealthStatus);

        var ne107Label = new Span("NE 107 Device Status");
        ne107Label.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        ne107Badge = new Span("—");
        ne107Badge.getStyle()
            .set("padding", "4px 12px")
            .set("border-radius", "4px")
            .set("font-weight", "bold")
            .set("color", "white")
            .set("background", "var(--lumo-secondary-text-color)");
        section.add(ne107Label, ne107Badge);

        return section;
    }

    private HorizontalLayout createMetricRow(String label) {
        var labelSpan = new Span(label);
        labelSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        var valueSpan = new Span("—");
        valueSpan.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        var row = new HorizontalLayout(labelSpan, valueSpan);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        return row;
    }

    private Span getMetricSpan(HorizontalLayout row) {
        return (Span) row.getChildren()
            .filter(c -> c instanceof Span)
            .reduce((a, b) -> b)
            .orElse(new Span("—"));
    }

    private void startPolling() {
        refreshTimer = new Timer("OPC-UA-Visualization-Poller", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    com.vaadin.flow.component.UI ui = getUI().orElse(null);
                    if (ui != null) {
                        ui.access(() -> refreshData());
                    }
                } catch (Exception e) {
                    // UI may have been detached
                }
            }
        }, 0, 2000);
    }

    private void refreshData() {
        try {
            OpcUaSnapshot snapshot = visualizationService.getCurrentSnapshot();
            if (snapshot != null) {
                updateConnectionStatus(true);

                if (snapshot.tankLevel() != null) {
                    tankLevelValue.setText(String.format("%.1f", snapshot.tankLevel()));
                }
                if (snapshot.tankFlowRateIn() != null) {
                    flowInValue.setText(String.format("%.2f", snapshot.tankFlowRateIn()));
                }
                if (snapshot.tankFlowRateOut() != null) {
                    flowOutValue.setText(String.format("%.2f", snapshot.tankFlowRateOut()));
                }
                if (snapshot.tankTemperature() != null) {
                    String tempText = String.format("%.1f", snapshot.tankTemperature());
                    temperatureValue.setText(tempText);
                    double temp = snapshot.tankTemperature();
                    if (temp > 40) {
                        temperatureValue.getStyle().set("color", "var(--lumo-error-color)");
                    } else if (temp > 35) {
                        temperatureValue.getStyle().set("color", "var(--lumo-warning-color)");
                    } else {
                        temperatureValue.getStyle().set("color", "var(--lumo-success-color)");
                    }
                }
                if (snapshot.pumpFlowRate() != null) {
                    pumpFlowValue.setText(String.format("%.2f", snapshot.pumpFlowRate()));
                }
                if (snapshot.pumpVibration() != null) {
                    String vibText = String.format("%.2f", snapshot.pumpVibration());
                    pumpVibrationValue.setText(vibText);
                    double vib = snapshot.pumpVibration();
                    if (vib > 8) {
                        pumpVibrationValue.getStyle().set("color", "var(--lumo-error-color)");
                    } else if (vib > 5) {
                        pumpVibrationValue.getStyle().set("color", "var(--lumo-warning-color)");
                    } else {
                        pumpVibrationValue.getStyle().set("color", "var(--lumo-success-color)");
                    }
                }

                if (snapshot.pumpDeviceStatus() != null) {
                    pumpHealthStatus.setText(snapshot.pumpDeviceStatus());
                    ne107Badge.setText(snapshot.pumpDeviceStatus());
                    ne107Badge.getStyle().set("background", getNe107Color(snapshot.pumpDeviceStatus()));
                }
            } else {
                updateConnectionStatus(false);
            }
        } catch (Exception e) {
            updateConnectionStatus(false);
        }
    }

    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            connectionStatus.setText("Connected");
            connectionStatus.getStyle().set("background", "var(--lumo-success-color)");
        } else {
            connectionStatus.setText("Disconnected");
            connectionStatus.getStyle().set("background", "var(--lumo-error-color)");
        }
    }

    private String getNe107Color(String status) {
        return switch (status) {
            case "Good" -> "var(--lumo-success-color)";
            case "Maintenance", "Early wear detected" -> "var(--lumo-warning-color)";
            case "Failure", "Device failure" -> "var(--lumo-error-color)";
            default -> "var(--lumo-secondary-text-color)";
        };
    }

    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }
}
