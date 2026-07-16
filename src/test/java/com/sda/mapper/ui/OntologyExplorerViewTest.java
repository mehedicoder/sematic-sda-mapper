package com.sda.mapper.ui;

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
    @DisplayName("OntologyNode record should hold name, type, description, children")
    void shouldCreateOntologyNode() {
        var node = new OntologyExplorerView.OntologyNode(
            "Service", "Class", "Service class", java.util.List.of());
        assertEquals("Service", node.name());
        assertEquals("Class", node.type());
        assertEquals("Service class", node.description());
        assertTrue(node.children().isEmpty());
    }

    @Test
    @DisplayName("OntologyNode should support nested children")
    void shouldSupportNestedChildren() {
        var child = new OntologyExplorerView.OntologyNode(
            "OfferedCapability", "SubClass", "offered", java.util.List.of());
        var parent = new OntologyExplorerView.OntologyNode(
            "Capability", "Class", "hierarchy", java.util.List.of(child));
        assertEquals(1, parent.children().size());
        assertEquals("OfferedCapability", parent.children().getFirst().name());
    }

    @Test
    @DisplayName("buildOntologyTree should return root with 6 branches")
    void shouldBuildOntologyTree() {
        var root = OntologyExplorerView.buildOntologyTree();
        assertNotNull(root);
        assertEquals("CSS-SDA Model", root.name());
        assertEquals(6, root.children().size(), "Root must have 6 branches");
    }

    @Test
    @DisplayName("Tree should contain Capability branch with OfferedCapability and RequiredCapability")
    void shouldContainCapabilityBranch() {
        var root = OntologyExplorerView.buildOntologyTree();
        var capability = root.children().stream()
            .filter(n -> n.name().equals("Capability"))
            .findFirst();
        assertTrue(capability.isPresent(), "Must contain Capability branch");
        assertEquals(2, capability.get().children().size());
    }

    @Test
    @DisplayName("Tree should contain NE 107 Diagnostics branch")
    void shouldContainNe107Branch() {
        var root = OntologyExplorerView.buildOntologyTree();
        var ne107 = root.children().stream()
            .filter(n -> n.name().equals("NE 107 Diagnostics"))
            .findFirst();
        assertTrue(ne107.isPresent(), "Must contain NE 107 Diagnostics branch");
        assertEquals(2, ne107.get().children().size());
    }

    @Test
    @DisplayName("Tree should contain Instances branch with 3 use cases")
    void shouldContainInstancesBranch() {
        var root = OntologyExplorerView.buildOntologyTree();
        var instances = root.children().stream()
            .filter(n -> n.name().equals("Instances"))
            .findFirst();
        assertTrue(instances.isPresent(), "Must contain Instances branch");
        assertEquals(3, instances.get().children().size(), "Must have 3 use cases");
    }
}
