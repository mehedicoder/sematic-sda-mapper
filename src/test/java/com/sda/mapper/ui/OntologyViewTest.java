package com.sda.mapper.ui;

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
        assertEquals("visualizer", route.value());
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
    @DisplayName("Should expose all 24 classes of the canonical TBox")
    void shouldExposeAllClasses() {
        OntologyDataService service = new OntologyDataService();
        assertEquals(24, service.getClasses().size());
        assertEquals("1.1", service.getClasses().get(0).code());
        assertEquals("Observation", service.getClasses().get(23).name());
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
    @DisplayName("Should expose all 62 relations of the canonical TBox")
    void shouldExposeAllRelations() {
        OntologyDataService service = new OntologyDataService();
        List<OntologyRelation> relations = service.getRelations();
        assertEquals(62, relations.size());
        long core = relations.stream().filter(r -> "core".equals(r.kind())).count();
        long supporting = relations.stream().filter(r -> "supporting".equals(r.kind())).count();
        assertEquals(15, core);
        assertEquals(47, supporting);
    }

    @Test
    @DisplayName("Should resolve classes and layers by name")
    void shouldResolveClassesAndLayersByName() {
        OntologyDataService service = new OntologyDataService();
        assertTrue(service.getClassByName("Requirement").isPresent());
        assertEquals("1.3", service.getClassByName("Requirement").orElseThrow().code());
        assertTrue(service.getClassByName("NotAClass").isEmpty());
        assertTrue(service.getLayerById("L2b").isPresent());
        assertTrue(service.getLayerById("Nope").isEmpty());
    }

    @Test
    @DisplayName("Should expose outgoing and incoming relations for a class")
    void shouldExposeOutgoingAndIncomingRelations() {
        OntologyDataService service = new OntologyDataService();
        List<OntologyRelation> outgoing = service.getRelationsFrom("Service");
        assertEquals(6, outgoing.size());
        assertTrue(outgoing.stream().anyMatch(r -> "satisfies".equals(r.label())), "Service satisfies Requirement");
        assertTrue(outgoing.stream().anyMatch(r -> "instantiatedAs".equals(r.label())));

        List<OntologyRelation> incoming = service.getRelationsTo("Service");
        assertEquals(6, incoming.size());
        assertTrue(incoming.stream().anyMatch(r -> "exposedAs".equals(r.label())), "Capability exposes Service");
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
        assertEquals(24, items, "All layers tab should list all 24 classes");
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
        view.selectClass("Requirement");
        VerticalLayout detailPanel = getField(view, "detailPanel", VerticalLayout.class);
        String text = fullText(detailPanel.getElement());
        assertTrue(text.contains("Neighborhood Graph"));
        assertTrue(text.contains("mapsToCapability"),
            "Outgoing relation card should mention mapsToCapability");
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
        view.selectClass("Requirement");
        view.selectClass("Skill");
        VerticalLayout detailPanel = getField(view, "detailPanel", VerticalLayout.class);
        String text = fullText(detailPanel.getElement());
        assertTrue(text.contains("requiresConcept"));
        assertTrue(text.contains("realises"));
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
