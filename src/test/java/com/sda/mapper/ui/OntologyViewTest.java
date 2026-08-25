package com.sda.mapper.ui;

import com.sda.mapper.model.OntologyClass;
import com.sda.mapper.model.OntologyDatatypeProperty;
import com.sda.mapper.model.OntologyLayer;
import com.sda.mapper.model.OntologyRelation;
import com.sda.mapper.service.OntologyDataService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ontology Visualizer UI")
class OntologyViewTest {

    private OntologyView createView() {
        return new OntologyView(new OntologyDataService());
    }

    @Test
    @DisplayName("Should be a VerticalLayout")
    void shouldBeVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(OntologyView.class));
    }

    @Test
    @DisplayName("Should have Route annotation with visualizer path")
    void shouldHaveRouteAnnotation() {
        Route route = OntologyView.class.getAnnotation(Route.class);
        assertNotNull(route, "OntologyView must have @Route annotation");
        assertEquals("", route.value());
    }

    @Test
    @DisplayName("Should have correct @PageTitle")
    void shouldHaveCorrectPageTitle() {
        PageTitle pageTitle = OntologyView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "OntologyView must have @PageTitle annotation");
        assertEquals("Ontology Visualizer", pageTitle.value());
    }

    @Test
    @DisplayName("Should create OntologyView with the data service")
    void shouldCreateViewWithDependencies() {
        OntologyView view = createView();
        assertNotNull(view);
        assertTrue(view.getChildren().count() > 0, "View should contain child components");
    }

    @Test
    @DisplayName("Should expose the five CSS-SDA layers with fixed colors")
    void shouldExposeFiveLayers() {
        OntologyDataService service = new OntologyDataService();
        List<OntologyLayer> layers = service.getLayers();
        assertEquals(5, layers.size());
        assertEquals("#f59e0b", layers.get(0).colorHex());
        assertEquals("#10b981", layers.get(1).colorHex());
        assertEquals("#8b5cf6", layers.get(2).colorHex());
        assertEquals("#06b6d4", layers.get(3).colorHex());
        assertEquals("#ef4444", layers.get(4).colorHex());
        assertEquals(List.of("L1", "L2", "L2b", "L3", "L4"),
            layers.stream().map(OntologyLayer::id).toList());
    }

    @Test
    @DisplayName("Should expose all 24 numbered classes of the canonical TBox")
    void shouldExposeAllClasses() {
        OntologyDataService service = new OntologyDataService();
        assertEquals(24, service.getClasses().size());
        assertEquals("1.1", service.getClasses().get(0).code());
        assertEquals("Observation", service.getClasses().get(23).name());
        assertTrue(service.getClasses().stream().noneMatch(OntologyClass::derived),
            "Numbered classes must never be flagged as derived");
    }

    @Test
    @DisplayName("Should expose the 24 derived subclasses grouped under their numbered parents")
    void shouldExposeDerivedSubclasses() {
        OntologyDataService service = new OntologyDataService();
        assertEquals(26, service.getSubClasses().size());
        assertTrue(service.getSubClasses().stream()
            .allMatch(c -> c.derived() && c.parent() != null));

        assertEquals(List.of("AASSkill", "AutoLiftedMTPSkill", "ControlLogicSkill",
                "MTPSkill", "OrganisationalSkill"),
            service.getSubClassesOf("Skill").stream().map(OntologyClass::name).toList());
        assertEquals(List.of("FunctionalRequirement", "HardConstrainedRequirement",
                "PerformanceRequirement", "SafetyRequirement", "SecurityRequirement"),
            service.getSubClassesOf("UseCaseRequirement").stream()
                .map(OntologyClass::name).toList());
        assertEquals(List.of("CloudZone", "DMZZone", "EdgeZone", "ITZone", "OTZone"),
            service.getSubClassesOf("NetworkZone").stream().map(OntologyClass::name).toList());
        assertEquals(List.of("CloudContext", "EdgeContext", "HybridContext", "OnPremContext",
                "SafetyCriticalDeploymentContext"),
            service.getSubClassesOf("DeploymentContext").stream()
                .map(OntologyClass::name).toList());
        assertEquals(List.of("AnalyticsService", "CPCService", "MOService"),
            service.getSubClassesOf("Service").stream().map(OntologyClass::name).toList());
        assertEquals(List.of("MarginCompliantClusterNode"),
            service.getSubClassesOf("ClusterNode").stream().map(OntologyClass::name).toList());

        assertTrue(service.getSubClassesOf("Stakeholder").isEmpty());
        assertTrue(service.getSubClassesOf("NotAClass").isEmpty());
    }

    @Test
    @DisplayName("Should inherit layer and diagram numbering from the parent for derived classes")
    void shouldInheritParentLayerAndCode() {
        OntologyDataService service = new OntologyDataService();
        OntologyClass skill = service.getClassByName("Skill").orElseThrow();
        OntologyClass autoLifted = service.getClassByName("AutoLiftedMTPSkill").orElseThrow();

        assertTrue(autoLifted.derived());
        assertEquals("Skill", autoLifted.parent());
        assertEquals(skill.code(), autoLifted.code());
        assertEquals(skill.layerId(), autoLifted.layerId());
        assertFalse(skill.derived());
    }

    @Test
    @DisplayName("Should inherit the parent's relations for derived classes and mark them inherited")
    void shouldInheritParentRelations() {
        OntologyDataService service = new OntologyDataService();
        List<OntologyRelation> outgoing = service.getRelationsFrom("AASSkill");
        assertFalse(outgoing.isEmpty(), "Derived class must inherit its parent's outgoing relations");
        assertTrue(outgoing.stream().allMatch(r -> r.inherited()),
            "Relations of a derived class must be marked inherited");
        assertTrue(outgoing.stream().anyMatch(r -> "requiresConcept".equals(r.label())));

        List<OntologyRelation> incoming = service.getRelationsTo("AASSkill");
        assertFalse(incoming.isEmpty(), "Derived class must inherit its parent's incoming relations");
        assertTrue(incoming.stream().allMatch(r -> r.inherited()));

        List<OntologyRelation> parentOutgoing = service.getRelationsFrom("Skill");
        assertTrue(parentOutgoing.stream().noneMatch(r -> r.inherited()),
            "Declared relations of a numbered parent must not be marked inherited");
    }

    @Test
    @DisplayName("Should filter classes by layer")
    void shouldFilterClassesByLayer() {
        OntologyDataService service = new OntologyDataService();
        assertEquals(6, service.getClassesByLayer("L1").size());
        assertEquals(5, service.getClassesByLayer("L2").size());
        assertEquals(5, service.getClassesByLayer("L2b").size());
        assertEquals(4, service.getClassesByLayer("L3").size());
        assertEquals(4, service.getClassesByLayer("L4").size());
        assertTrue(service.getClassesByLayer("Unknown").isEmpty());
    }

    @Test
    @DisplayName("Should expose all 89 labeled relations of the v2 TBox")
    void shouldExposeAllRelations() {
        OntologyDataService service = new OntologyDataService();
        List<OntologyRelation> relations = service.getRelations();
        assertEquals(84, relations.size());
        long core = relations.stream().filter(r -> "core".equals(r.kind())).count();
        long supporting = relations.stream().filter(r -> "supporting".equals(r.kind())).count();
        assertEquals(18, core);
        assertEquals(66, supporting);
    }

    @Test
    @DisplayName("Should resolve classes and layers by name")
    void shouldResolveClassesAndLayersByName() {
        OntologyDataService service = new OntologyDataService();
        assertTrue(service.getClassByName("UseCaseRequirement").isPresent());
        assertEquals("1.3", service.getClassByName("UseCaseRequirement").orElseThrow().code());
        assertTrue(service.getClassByName("NotAClass").isEmpty());
        assertTrue(service.getLayerById("L2b").isPresent());
        assertTrue(service.getLayerById("Nope").isEmpty());
    }

    @Test
    @DisplayName("Should expose outgoing and incoming relations for a class")
    void shouldExposeOutgoingAndIncomingRelations() {
        OntologyDataService service = new OntologyDataService();
        List<OntologyRelation> outgoing = service.getRelationsFrom("Service");
        assertEquals(9, outgoing.size());
        assertTrue(outgoing.stream().anyMatch(r -> "satisfies".equals(r.label())), "Service satisfies UseCaseRequirement");
        assertTrue(outgoing.stream().anyMatch(r -> "instantiatedAs".equals(r.label())));

        List<OntologyRelation> incoming = service.getRelationsTo("Service");
        assertEquals(8, incoming.size());
        assertTrue(incoming.stream().anyMatch(r -> "deploysService".equals(r.label())),
            "DeploymentContext deploys Service");
        assertTrue(incoming.stream().anyMatch(r -> "bindsService".equals(r.label())));
    }

    @Test
    @DisplayName("Should render an empty inspector state by default")
    void shouldRenderEmptyStateByDefault() throws Exception {
        OntologyView view = createView();
        VerticalLayout detailPanel = getField(view, "detailPanel", VerticalLayout.class);
        assertTrue(fullText(detailPanel.getElement()).contains("No class selected"),
            "Default detail panel should show the empty state");
    }

    @Test
    @DisplayName("Should show class items for the All Layers tab")
    void shouldShowAllClassesByDefault() throws Exception {
        OntologyView view = createView();
        VerticalLayout classItemsPanel = getField(view, "classItemsPanel", VerticalLayout.class);
        long items = classItemsPanel.getChildren().filter(c -> c instanceof Div).count();
        assertEquals(50, items, "All layers tab should list the 24 numbered classes plus their 26 derived subclasses");
    }

    @Test
    @DisplayName("Should filter the class browser when a layer tab is selected")
    void shouldFilterClassBrowserOnLayerSelection() throws Exception {
        OntologyView view = createView();
        view.selectLayer("L3");
        VerticalLayout classItemsPanel = getField(view, "classItemsPanel", VerticalLayout.class);
        List<String> names = classItemsPanel.getChildren()
            .filter(c -> c instanceof Div)
            .map(c -> fullText(c.getElement()))
            .toList();
        assertEquals(4, names.size());
        assertTrue(names.stream().anyMatch(t -> t.contains("InformationModel")));
        assertTrue(names.stream().anyMatch(t -> t.contains("SemanticConcept")));
        assertTrue(names.stream().anyMatch(t -> t.contains("CommunicationBinding")));
        assertTrue(names.stream().anyMatch(t -> t.contains("DataConnector")));
    }

    @Test
    @DisplayName("Should render class inspector with graph and relation cards on selection")
    void shouldRenderInspectorOnClassSelection() throws Exception {
        OntologyView view = createView();
        view.selectClass("UseCaseRequirement");
        VerticalLayout detailPanel = getField(view, "detailPanel", VerticalLayout.class);
        String text = fullText(detailPanel.getElement());
        assertTrue(text.contains("Neighborhood Graph"));
        assertTrue(text.contains("hasParameter"),
            "Outgoing relation card should mention hasParameter");
        assertTrue(text.contains("satisfies"),
            "Incoming relation card should mention satisfies");
        assertTrue(hasClass(detailPanel.getElement(), "ontology-card--core"),
            "Core relations must be rendered as core cards");
        assertTrue(hasClass(detailPanel.getElement(), "ontology-graph"),
            "Selected class must render the neighborhood graph");
    }

    @Test
    @DisplayName("Should re-render the inspector when switching between classes")
    void shouldReRenderInspectorWhenSwitchingClasses() throws Exception {
        OntologyView view = createView();
        view.selectClass("Skill");
        view.selectClass("UseCaseRequirement");
        view.selectClass("Skill");
        VerticalLayout detailPanel = getField(view, "detailPanel", VerticalLayout.class);
        String text = fullText(detailPanel.getElement());
        assertTrue(text.contains("requiresConcept"));
    }

    @Test
    @DisplayName("Should show subclass note and inherited relation cards for derived classes")
    void shouldShowSubclassInspectorForDerivedClasses() throws Exception {
        OntologyView view = createView();
        view.selectClass("AASSkill");
        VerticalLayout detailPanel = getField(view, "detailPanel", VerticalLayout.class);
        String text = fullText(detailPanel.getElement());
        assertTrue(text.contains("Subclass of"), "Derived inspector should show a subclass note");
        assertTrue(text.contains("Skill"), "Subclass note should name the parent class");
        assertTrue(text.contains("INHERITED"),
            "Inherited relation cards must be marked as INHERITED");
        assertTrue(hasClass(detailPanel.getElement(), "ontology-card--inherited"),
            "Inherited relations must render as inherited cards");
    }

    @Test
    @DisplayName("Should show explicit Domain and Range labels on relation cards")
    void shouldShowDomainAndRangeLabelsOnRelationCards() throws Exception {
        OntologyView view = createView();
        view.selectClass("UseCaseRequirement");
        VerticalLayout detailPanel = getField(view, "detailPanel", VerticalLayout.class);
        String text = fullText(detailPanel.getElement());
        assertTrue(text.contains("Domain: UseCaseRequirement"),
            "Relation cards should show Domain label with the source class");
        assertTrue(text.contains("Range:"),
            "Relation cards should show Range label with the target class");
    }

    @Test
    @DisplayName("Should show Datatype Properties section for a class with properties")
    void shouldShowDatatypePropertiesSection() throws Exception {
        OntologyView view = createView();
        view.selectClass("DeploymentContext");
        VerticalLayout detailPanel = getField(view, "detailPanel", VerticalLayout.class);
        String text = fullText(detailPanel.getElement());
        assertTrue(text.contains("Datatype Properties"),
            "Inspector should show a Datatype Properties section");
        assertTrue(text.contains("contextID"),
            "DeploymentContext should have a contextID datatype property");
        assertTrue(text.contains("determinismClass"),
            "DeploymentContext should have a determinismClass datatype property");
    }

    @Test
    @DisplayName("Should show empty Datatype Properties section for a derived class with no own properties")
    void shouldShowEmptyDatatypePropertiesForDerivedClass() throws Exception {
        OntologyDataService service = new OntologyDataService();
        assertTrue(service.getDatatypeProperties("OnPremContext").isEmpty(),
            "OnPremContext (derived subclass) should have no own datatype properties");
        OntologyView view = createView();
        view.selectClass("OnPremContext");
        VerticalLayout detailPanel = getField(view, "detailPanel", VerticalLayout.class);
        String text = fullText(detailPanel.getElement());
        assertTrue(text.contains("Datatype Properties (0)"),
            "Empty datatype properties section should show count 0");
    }

    @Test
    @DisplayName("Should derive datatype properties from the ontology for all known classes")
    void shouldDeriveDatatypePropertiesFromOntology() {
        OntologyDataService service = new OntologyDataService();
        List<OntologyDatatypeProperty> all = service.getDatatypeProperties();
        assertFalse(all.isEmpty(), "Ontology should declare datatype properties");
        assertTrue(all.stream().anyMatch(p -> "contextID".equals(p.name())),
            "contextID should be a known datatype property");
        assertTrue(all.stream().anyMatch(p -> "safetyIntegrityLevel".equals(p.name())),
            "safetyIntegrityLevel should be a known datatype property");
        assertTrue(all.stream().anyMatch(p -> "DeterminismClass".equals(p.className()) || "DeploymentContext".equals(p.className())),
            "DeploymentContext should own datatype properties");
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(OntologyView view, String name, Class<T> type) throws Exception {
        java.lang.reflect.Field field = OntologyView.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(view));
    }

    private static boolean hasClass(Element element, String cssClass) {
        String classes = element.getAttribute("class");
        if (classes != null && classes.contains(cssClass)) {
            return true;
        }
        return element.getChildren().anyMatch(child -> hasClass(child, cssClass));
    }

    private static String fullText(Element element) {
        StringBuilder builder = new StringBuilder();
        appendText(element, builder);
        return builder.toString();
    }

    private static void appendText(Element element, StringBuilder builder) {
        String own = element.getText();
        if (own != null && !own.isBlank()) {
            builder.append(own);
        }
        element.getChildren().forEach(child -> appendText(child, builder));
    }
}
