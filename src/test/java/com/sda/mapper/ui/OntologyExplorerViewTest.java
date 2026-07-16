package com.sda.mapper.ui;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ontology Explorer View UI")
class OntologyExplorerViewTest {

    @Test
    @DisplayName("Should define OntologyExplorerView extending VerticalLayout")
    void shouldExtendVerticalLayout() {
        assertTrue(com.vaadin.flow.component.orderedlayout.VerticalLayout.class
            .isAssignableFrom(OntologyExplorerView.class));
    }

    @Test
    @DisplayName("OntologyNode record should hold name, type, uri, description, children")
    void shouldCreateOntologyNode() {
        var node = new OntologyExplorerView.OntologyNode(
            "Service", "Class", "http://example.org/sda#Service", "Service class", java.util.List.of());
        assertEquals("Service", node.name());
        assertEquals("Class", node.type());
        assertEquals("http://example.org/sda#Service", node.uri());
        assertEquals("Service class", node.description());
        assertTrue(node.children().isEmpty());
    }

    @Test
    @DisplayName("OntologyNode should support nested children")
    void shouldSupportNestedChildren() {
        var child = new OntologyExplorerView.OntologyNode(
            "OfferedCapability", "SubClass", "", "offered", java.util.List.of());
        var parent = new OntologyExplorerView.OntologyNode(
            "Capability", "Class", "", "hierarchy", java.util.List.of(child));
        assertEquals(1, parent.children().size());
        assertEquals("OfferedCapability", parent.children().getFirst().name());
    }

    @Test
    @DisplayName("Should create view with OntologyModelLoader dependency")
    void shouldCreateViewWithLoader() {
        var loader = new OntologyModelLoader();
        var view = new OntologyExplorerView(loader);
        assertNotNull(view);
        assertTrue(view.getClass().isAssignableFrom(OntologyExplorerView.class));
    }

    @Test
    @DisplayName("Should create view with empty model gracefully")
    void shouldHandleEmptyModel() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        var view = new OntologyExplorerView(loader);
        assertNotNull(view);
    }
}
