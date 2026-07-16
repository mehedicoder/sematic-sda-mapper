package com.sda.mapper.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

/**
 * Explorer view for browsing the CSS-SDA ontology model.
 * Shows a tree grid of ontology classes on the left and details on the right.
 */
@Route(value = "ontology", layout = MainLayout.class)
@PageTitle("Ontology Explorer")
public class OntologyExplorerView extends VerticalLayout {

    private final TreeGrid<OntologyNode> treeGrid;
    private final VerticalLayout detailsPanel;
    private boolean treePopulated;

    /**
     * Represents a node in the ontology tree.
     */
    public record OntologyNode(String name, String type, String description, List<OntologyNode> children) {
    }

    public OntologyExplorerView() {
        setSizeFull();
        setPadding(false);

        var header = new H2("Ontology Explorer");
        header.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        treeGrid = new TreeGrid<>();
        treeGrid.addHierarchyColumn(OntologyNode::name)
            .setHeader("Class / Instance")
            .setFlexGrow(1);
        treeGrid.addColumn(OntologyNode::type)
            .setHeader("Type")
            .setWidth("150px");
        treeGrid.setWidth("400px");

        detailsPanel = new VerticalLayout();
        detailsPanel.setPadding(true);
        detailsPanel.add(new H3("Select a node"));
        detailsPanel.add(new Paragraph("Click on an ontology node to view its properties."));

        var splitLayout = new SplitLayout(treeGrid, detailsPanel);
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(40);

        add(header, splitLayout);
        setFlexGrow(1, splitLayout);

        treeGrid.addSelectionListener(event -> {
            detailsPanel.removeAll();
            var selected = event.getFirstSelectedItem();
            if (selected.isPresent()) {
                var node = selected.get();
                detailsPanel.add(new H3(node.name()));
                detailsPanel.add(new Span("Type: " + node.type()));
                detailsPanel.add(new Paragraph(node.description()));
                if (!node.children().isEmpty()) {
                    detailsPanel.add(new Paragraph("Children: " + node.children().size()));
                }
            } else {
                detailsPanel.add(new H3("Select a node"));
                detailsPanel.add(new Paragraph("Click on an ontology node to view its properties."));
            }
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!treePopulated) {
            populateTree();
            treePopulated = true;
        }
    }

    private void populateTree() {
        var cssSda = buildOntologyTree();
        treeGrid.setItems(List.of(cssSda), OntologyNode::children);
        treeGrid.expand(cssSda);
    }

    static OntologyNode buildOntologyTree() {
        return new OntologyNode("CSS-SDA Model", "Ontology", "CSS-SDA ontology model based on the paper", List.of(
            createCapabilityBranch(),
            createSkillBranch(),
            createServiceBranch(),
            createPhysicalAssetsBranch(),
            createNe107Branch(),
            createInstancesBranch()
        ));
    }

    private static OntologyNode createCapabilityBranch() {
        return new OntologyNode("Capability", "Class", "Capability class hierarchy", List.of(
            new OntologyNode("OfferedCapability", "SubClass", "Capability offered by a service", List.of()),
            new OntologyNode("RequiredCapability", "SubClass", "Capability required by a requirement", List.of())
        ));
    }

    private static OntologyNode createSkillBranch() {
        return new OntologyNode("Skill", "Class", "Skill class - implements a capability", List.of(
            new OntologyNode("DegradedMode", "SubClass", "Degraded operation mode (NE 107)", List.of())
        ));
    }

    private static OntologyNode createServiceBranch() {
        return new OntologyNode("Service", "Class", "Service class - implements a skill", List.of(
            new OntologyNode("CommunicationBinding", "Class", "Protocol binding for a service", List.of()),
            new OntologyNode("DataConnector", "Class", "Data connector for a binding", List.of()),
            new OntologyNode("DeploymentContext", "Class", "Deployment context for a service", List.of())
        ));
    }

    private static OntologyNode createPhysicalAssetsBranch() {
        return new OntologyNode("Physical Assets", "Classes", "Physical asset classes", List.of(
            new OntologyNode("MTPModule", "Class", "MTP Module (Module Type Package)", List.of()),
            new OntologyNode("FieldDevice", "Class", "Field device (sensor, actuator)", List.of(
                new OntologyNode("BACnetDevice", "SubClass", "BACnet-connected device", List.of()),
                new OntologyNode("EnergyMeter", "SubClass", "Energy metering device", List.of())
            ))
        ));
    }

    private static OntologyNode createNe107Branch() {
        return new OntologyNode("NE 107 Diagnostics", "Classes", "NAMUR NE 107 diagnostic classes", List.of(
            new OntologyNode("NE107Status", "Class", "NE 107 status code", List.of(
                new OntologyNode("NE107-Good", "Individual", "Status: Good", List.of()),
                new OntologyNode("NE107-Maintenance", "Individual", "Status: Maintenance required", List.of()),
                new OntologyNode("NE107-Failure", "Individual", "Status: Failure", List.of())
            )),
            new OntologyNode("NE107SubCode", "Class", "NE 107 sub-code", List.of(
                new OntologyNode("NE107-Sensor", "Individual", "Sub-code: Sensor failure", List.of()),
                new OntologyNode("NE107-WireBreak", "Individual", "Sub-code: Wire break", List.of()),
                new OntologyNode("NE107-ShortCircuit", "Individual", "Sub-code: Short circuit", List.of()),
                new OntologyNode("NE107-Function", "Individual", "Sub-code: Function failure", List.of())
            ))
        ));
    }

    private static OntologyNode createInstancesBranch() {
        return new OntologyNode("Instances", "Individuals", "Use case instances", List.of(
            new OntologyNode("Use Case A: Flowmeter NE 107", "Use Case", "Asset condition monitoring", List.of(
                new OntologyNode("REQ-001", "Requirement", "Flowmeter Asset-Health requirement", List.of()),
                new OntologyNode("SVC-FlowmeterDiagnostics", "Service", "Flowmeter diagnostics service", List.of()),
                new OntologyNode("CB-Flowmeter-OPCUA", "Binding", "OPC-UA read-only binding", List.of()),
                new OntologyNode("DC-Flowmeter-FT101", "Connector", "FT101 flowmeter connector", List.of()),
                new OntologyNode("DEPLOY-OnPrem", "Context", "On-premise deployment", List.of())
            )),
            new OntologyNode("Use Case B: Fermentation Skid", "Use Case", "Critical process control", List.of(
                new OntologyNode("REQ-002", "Requirement", "Fermentation Temperature Control", List.of()),
                new OntologyNode("SVC-FermentationSkid", "Service", "Fermentation skid service", List.of()),
                new OntologyNode("Skid-A1", "MTPModule", "Fermentation skid physical asset", List.of()),
                new OntologyNode("DEPLOY-Critical", "Context", "Critical process deployment", List.of())
            )),
            new OntologyNode("Use Case C: HVAC Plant", "Use Case", "Non-critical monitoring", List.of(
                new OntologyNode("REQ-003", "Requirement", "HVAC Temperature Monitoring", List.of()),
                new OntologyNode("SVC-HVACTempAnalysis", "Service", "HVAC temperature analysis", List.of()),
                new OntologyNode("AHU-01", "BACnetDevice", "Air handling unit", List.of()),
                new OntologyNode("EM-01", "EnergyMeter", "Energy meter", List.of()),
                new OntologyNode("DEPLOY-OTZone3", "Context", "OT Zone 3 deployment", List.of())
            ))
        ));
    }
}
