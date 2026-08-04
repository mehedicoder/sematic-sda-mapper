package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.service.InferenceService;
import com.sda.mapper.hub.service.LiftService;
import com.sda.mapper.hub.service.ManifestExportService;
import com.sda.mapper.hub.service.MappingHubService;
import com.sda.mapper.hub.service.MappingHubSession;
import com.sda.mapper.hub.service.MatchingService;
import com.sda.mapper.hub.service.RecommendationService;
import com.sda.mapper.hub.service.SemanticMatchService;
import com.sda.mapper.hub.parser.AasModelParser;
import com.sda.mapper.hub.parser.CanonicalParserRegistry;
import com.sda.mapper.hub.parser.HubFileTypeDetector;
import com.sda.mapper.hub.parser.MtpModelParser;
import com.sda.mapper.hub.parser.OpcUaModelParser;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.service.OntologyDataService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Mapping Hub View")
class MappingHubViewTest {

    private MappingHubView createView() {
        OntologyDataService ontologyDataService = new OntologyDataService();
        MatchingService matchingService = new MatchingService(ontologyDataService);
        RecommendationService recommendationService = new RecommendationService();
        MappingHubService hubService = new MappingHubService(
            new CanonicalParserRegistry(List.of(
                new AasModelParser(), new MtpModelParser(), new OpcUaModelParser())),
            new HubFileTypeDetector(),
            new LiftService(),
            new InferenceService(),
            matchingService,
            new SemanticMatchService(new SparqlQueryLoader()),
            recommendationService,
            new ManifestExportService(matchingService, recommendationService),
            new MappingHubSession());
        return new MappingHubView(hubService, ontologyDataService);
    }

    @Test
    @DisplayName("Should be a VerticalLayout")
    void shouldBeVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(MappingHubView.class));
    }

    @Test
    @DisplayName("Should have Route annotation with mapping-hub path and MainLayout")
    void shouldHaveRouteAnnotation() {
        Route route = MappingHubView.class.getAnnotation(Route.class);
        assertNotNull(route, "MappingHubView must have @Route annotation");
        assertEquals("mapping-hub", route.value());
        assertEquals(com.sda.mapper.ui.MainLayout.class, route.layout());
    }

    @Test
    @DisplayName("Should have correct @PageTitle")
    void shouldHaveCorrectPageTitle() {
        PageTitle pageTitle = MappingHubView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "MappingHubView must have @PageTitle annotation");
        assertEquals("Mapping Hub", pageTitle.value());
    }

    @Test
    @DisplayName("Should import the hub stylesheet")
    void shouldImportStylesheet() {
        CssImport cssImport = MappingHubView.class.getAnnotation(CssImport.class);
        assertNotNull(cssImport, "MappingHubView must import a stylesheet");
        assertEquals("./hub-styles.css", cssImport.value());
    }

    @Test
    @DisplayName("Should render the header and console on construction")
    void shouldRenderCoreComponents() throws Exception {
        MappingHubView view = createView();
        String text = fullText(view.getElement());
        assertTrue(text.contains("Semantic Mapping Hub"));
        assertTrue(text.contains("Re-running pipeline"));
    }

    @Test
    @DisplayName("Should list all 24 classes in the All Layers tab")
    void shouldListAllClassesByDefault() throws Exception {
        MappingHubView view = createView();
        ClassListPanel classListPanel = getField(view, "classListPanel", ClassListPanel.class);
        long rows = classListPanel.getChildren().filter(c -> c instanceof Div).count();
        assertEquals(24, rows);
    }

    @Test
    @DisplayName("Should expose the five layer tabs plus the All Layers tab")
    void shouldExposeSixLayerTabs() throws Exception {
        MappingHubView view = createView();
        LayerTabs layerTabs = getField(view, "layerTabs", LayerTabs.class);
        assertEquals(6, layerTabs.getChildren().count());
    }

    @Test
    @DisplayName("Should write a re-run log line to the console on construction")
    void shouldLogToConsoleOnConstruction() throws Exception {
        MappingHubView view = createView();
        ConsolePanel console = getField(view, "console", ConsolePanel.class);
        assertTrue(console.getLines().stream()
            .anyMatch(line -> line.contains("Re-running pipeline")));
    }

    @Test
    @DisplayName("Should select a class and refresh the detail panel")
    void shouldRefreshDetailOnClassSelection() throws Exception {
        MappingHubView view = createView();
        view.selectClass("Asset");
        ClassDetailPanel detailPanel = getField(view, "detailPanel", ClassDetailPanel.class);
        String text = fullText(detailPanel.getElement());
        assertTrue(text.contains("Asset"));
        assertTrue(text.contains("Gap actions"));
    }

    @Test
    @DisplayName("Should filter the class browser to the six L1 classes")
    void shouldFilterClassBrowserToL1() throws Exception {
        MappingHubView view = createView();
        view.selectLayer("L1");
        ClassListPanel classListPanel = getField(view, "classListPanel", ClassListPanel.class);
        long rows = classListPanel.getChildren().filter(c -> c instanceof Div).count();
        assertEquals(6, rows);
    }

    @Test
    @DisplayName("Should filter the class browser to the five L2 classes")
    void shouldFilterClassBrowserToL2() throws Exception {
        MappingHubView view = createView();
        view.selectLayer("L2");
        ClassListPanel classListPanel = getField(view, "classListPanel", ClassListPanel.class);
        long rows = classListPanel.getChildren().filter(c -> c instanceof Div).count();
        assertEquals(5, rows);
    }

    @Test
    @DisplayName("Should process an uploaded AAS file and refresh dashboard, console and export button")
    void shouldProcessUploadAndRefreshView() throws Exception {
        byte[] content;
        try (InputStream in = getClass().getResourceAsStream("/device-descriptions/aas/pump-aas.json")) {
            assertNotNull(in, "Fixture pump-aas.json not found");
            content = in.readAllBytes();
        }
        UI ui = new UI();
        UI.setCurrent(ui);
        try {
            MappingHubView view = createView();
            Method onUpload = MappingHubView.class.getDeclaredMethod(
                "onUpload", String.class, byte[].class);
            onUpload.setAccessible(true);
            onUpload.invoke(view, "pump-aas.json", content);

            ConsolePanel console = getField(view, "console", ConsolePanel.class);
            assertTrue(console.getLines().stream().anyMatch(line -> line.startsWith("[PARSE]")));
            assertTrue(console.getLines().stream().anyMatch(line -> line.startsWith("[MATCH]")));
            Button exportButton = getField(view, "exportButton", Button.class);
            assertTrue(exportButton.isEnabled());
        } finally {
            UI.setCurrent(null);
        }
    }

    private static <T> T getField(Object target, String name, Class<T> type) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
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
