package com.sda.mapper.ui;

import com.sda.mapper.error.OntologyLoadingException;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Explorer view for browsing the CSS-SDA ontology model.
 * Dynamically loads tree from the Apache Jena Model via {@link OntologyModelLoader}.
 * Shows a tree grid of ontology entities on the left and tabbed details on the right.
 */
@Route(value = "ontology", layout = MainLayout.class)
@PageTitle("Ontology Explorer")
public class OntologyExplorerView extends VerticalLayout {

    private final OntologyModelLoader ontologyModelLoader;
    private final TreeGrid<OntologyNode> treeGrid;
    private final VerticalLayout detailsPanel;
    private boolean treePopulated;

    /**
     * Represents a node in the ontology tree.
     */
    public record OntologyNode(String name, String type, String uri, String description,
                                List<OntologyNode> children) {
    }

    public OntologyExplorerView(OntologyModelLoader ontologyModelLoader) {
        this.ontologyModelLoader = ontologyModelLoader;
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
        detailsPanel.add(new Paragraph("Click on an ontology entity to view its details."));

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
                showNodeDetails(node);
            } else {
                detailsPanel.add(new H3("Select a node"));
                detailsPanel.add(new Paragraph("Click on an ontology entity to view its details."));
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
        if (!ontologyModelLoader.isLoaded()) {
            treeGrid.setItems(List.of(
                new OntologyNode("No ontology loaded", "Error",
                    "", "Ontology file could not be loaded. Check application logs.", List.of())
            ), OntologyNode::children);
            return;
        }

        try {
            Model model = ontologyModelLoader.getModel();
            var rootNode = buildDynamicTree(model);
            treeGrid.setItems(List.of(rootNode), OntologyNode::children);
            treeGrid.expand(rootNode);
        } catch (OntologyLoadingException e) {
            treeGrid.setItems(List.of(
                new OntologyNode("Ontology loading error", "Error",
                    "", e.getMessage(), List.of())
            ), OntologyNode::children);
        }
    }

    private static OntologyNode buildDynamicTree(Model model) {
        List<OntologyNode> children = new ArrayList<>();

        var classes = model.listSubjectsWithProperty(RDF.type, RDFS.Class).toList();
        var owlClasses = model.listSubjectsWithProperty(RDF.type, OWL.Class).toList();
        classes.addAll(owlClasses);

        Set<String> seenIndUris = new LinkedHashSet<>();

        for (var cls : classes) {
            if (!cls.isURIResource()) continue;
            String localName = getLocalName(cls);
            String label = getLabel(cls);
            String description = getDescription(cls);
            List<OntologyNode> individuals = new ArrayList<>();

            var instances = model.listSubjectsWithProperty(RDF.type, cls).toList();
            for (var inst : instances) {
                if (!inst.isURIResource()) continue;
                seenIndUris.add(inst.getURI());
                individuals.add(new OntologyNode(
                    getLocalName(inst),
                    "Individual",
                    inst.getURI(),
                    getLabel(inst),
                    List.of()
                ));
            }

            children.add(new OntologyNode(label, "Class", cls.getURI(), description, individuals));
        }

        var properties = model.listSubjectsWithProperty(RDF.type, RDF.Property).toList();
        var objProps = model.listSubjectsWithProperty(RDF.type, OWL.ObjectProperty).toList();
        var dataProps = model.listSubjectsWithProperty(RDF.type, OWL.DatatypeProperty).toList();
        properties.addAll(objProps);
        properties.addAll(dataProps);

        List<OntologyNode> propNodes = new ArrayList<>();
        Set<String> seenPropUris = new LinkedHashSet<>();
        for (var prop : properties) {
            if (!prop.isURIResource()) continue;
            String uri = prop.getURI();
            if (!seenPropUris.add(uri)) continue;
            String type = "Property";
            if (objProps.contains(prop)) type = "ObjectProperty";
            else if (dataProps.contains(prop)) type = "DatatypeProperty";
            propNodes.add(new OntologyNode(
                getLocalName(prop),
                type,
                uri,
                getLabel(prop),
                List.of()
            ));
        }

        var individuals = model.listSubjectsWithProperty(RDF.type).toList();
        List<OntologyNode> orphanNodes = new ArrayList<>();
        for (var ind : individuals) {
            if (!ind.isURIResource()) continue;
            var types = ind.listProperties(RDF.type).mapWith(s -> s.getObject().asResource());
            boolean isClass = false;
            while (types.hasNext()) {
                var t = types.next();
                if (t.equals(RDFS.Class) || t.equals(OWL.Class) || t.equals(RDF.Property)
                    || t.equals(OWL.ObjectProperty) || t.equals(OWL.DatatypeProperty)) {
                    isClass = true;
                    break;
                }
            }
            if (!isClass) {
                if (seenIndUris.contains(ind.getURI())) continue;
                String typeLabel = getTypesLabel(ind, model);
                orphanNodes.add(new OntologyNode(
                    getLocalName(ind),
                    typeLabel,
                    ind.getURI(),
                    getLabel(ind),
                    List.of()
                ));
            }
        }

        if (!propNodes.isEmpty()) {
            children.add(new OntologyNode("Properties", "Category", "",
                "Object and data properties defined in the ontology", propNodes));
        }

        if (!orphanNodes.isEmpty()) {
            children.add(new OntologyNode("Instances", "Category", "",
                "Individual instances not grouped under a class node above", orphanNodes));
        }

        return new OntologyNode("CSS-SDA Model", "Ontology", "",
            "CSS-SDA ontology model — " + model.size() + " triples", children);
    }

    private static String getTypesLabel(Resource resource, Model model) {
        var types = resource.listProperties(RDF.type).mapWith(s -> s.getObject().asResource()).toList();
        if (types.isEmpty()) return "Resource";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.size() && i < 2; i++) {
            if (i > 0) sb.append(", ");
            sb.append(getLocalName(types.get(i)));
        }
        return sb.toString();
    }

    private static String getLocalName(Resource resource) {
        String uri = resource.getURI();
        if (uri == null) return resource.toString();
        int hash = uri.lastIndexOf('#');
        if (hash >= 0) return uri.substring(hash + 1);
        int slash = uri.lastIndexOf('/');
        if (slash >= 0) return uri.substring(slash + 1);
        return uri;
    }

    private static String getLabel(Resource resource) {
        var labelStmt = resource.getProperty(RDFS.label);
        if (labelStmt != null && labelStmt.getString() != null) {
            return labelStmt.getString();
        }
        return getLocalName(resource);
    }

    private static String getDescription(Resource resource) {
        var commentStmt = resource.getProperty(RDFS.comment);
        if (commentStmt != null && commentStmt.getString() != null) {
            return commentStmt.getString();
        }
        return "";
    }

    private void showNodeDetails(OntologyNode node) {
        detailsPanel.add(new H3(node.name()));

        var tabs = new Tabs();
        var overviewTab = new Tab("Overview");
        var propertiesTab = new Tab("Properties");
        var relationshipsTab = new Tab("Relationships");
        tabs.add(overviewTab, propertiesTab, relationshipsTab);

        var overviewContent = createOverviewContent(node);
        var propertiesContent = createPropertiesContent(node);
        var relationshipsContent = createRelationshipsContent(node);

        propertiesContent.setVisible(false);
        relationshipsContent.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            overviewContent.setVisible(event.getSelectedTab() == overviewTab);
            propertiesContent.setVisible(event.getSelectedTab() == propertiesTab);
            relationshipsContent.setVisible(event.getSelectedTab() == relationshipsTab);
        });

        detailsPanel.add(tabs, overviewContent, propertiesContent, relationshipsContent);
    }

    private VerticalLayout createOverviewContent(OntologyNode node) {
        var layout = new VerticalLayout();
        layout.add(new Span("Type: " + node.type()));
        if (node.uri() != null && !node.uri().isEmpty()) {
            layout.add(new Span("URI: " + node.uri()));
        }
        if (node.description() != null && !node.description().isEmpty()) {
            layout.add(new Paragraph(node.description()));
        }
        if (!node.children().isEmpty()) {
            layout.add(new Span("Contains " + node.children().size() + " child entities"));
        }
        return layout;
    }

    private VerticalLayout createPropertiesContent(OntologyNode node) {
        var layout = new VerticalLayout();
        if (!ontologyModelLoader.isLoaded()) {
            layout.add(new Paragraph("Ontology not loaded"));
            return layout;
        }

        Model model = ontologyModelLoader.getModel();
        if (node.uri() == null || node.uri().isEmpty()) {
            layout.add(new Paragraph("No URI — properties not available"));
            return layout;
        }

        Resource resource = model.getResource(node.uri());
        var stmts = resource.listProperties().toList();
        if (stmts.isEmpty()) {
            layout.add(new Paragraph("No properties found"));
            return layout;
        }

        for (var stmt : stmts) {
            Property prop = stmt.getPredicate();
            RDFNode value = stmt.getObject();
            String propName = getLocalName(prop);
            String valueStr = value.isURIResource() ? getLocalName(value.asResource()) :
                value.isLiteral() ? value.asLiteral().getString() : value.toString();
            layout.add(new Span(propName + " = " + valueStr));
        }
        return layout;
    }

    private VerticalLayout createRelationshipsContent(OntologyNode node) {
        var layout = new VerticalLayout();
        if (!ontologyModelLoader.isLoaded()) {
            layout.add(new Paragraph("Ontology not loaded"));
            return layout;
        }

        Model model = ontologyModelLoader.getModel();
        if (node.uri() == null || node.uri().isEmpty()) {
            layout.add(new Paragraph("No URI — relationships not available"));
            return layout;
        }

        Resource resource = model.getResource(node.uri());
        var stmts = resource.listProperties().toList();
        if (stmts.isEmpty()) {
            layout.add(new Paragraph("No relationships found"));
            return layout;
        }

        for (var stmt : stmts) {
            if (stmt.getObject().isURIResource()) {
                String propName = getLocalName(stmt.getPredicate());
                String targetName = getLocalName(stmt.getObject().asResource());
                layout.add(new Span(propName + " → " + targetName));
            }
        }
        return layout;
    }
}
