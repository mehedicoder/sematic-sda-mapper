package com.sda.mapper.ui;

import com.sda.mapper.deployment.DockerComposeManifestService;
import com.sda.mapper.deployment.KubernetesManifestService;
import com.sda.mapper.reasoning.EndToEndPipelineService;
import com.sda.mapper.reasoning.OntologyInferenceService;
import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.RequirementDiscoveryService;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.parser.UploadService;
import com.sda.mapper.parser.ParserFactory;
import com.sda.mapper.parser.MTPAmlParser;
import com.sda.mapper.parser.UaServerDatasheetParser;
import com.sda.mapper.parser.OpcUaNodeSetParser;
import com.sda.mapper.parser.AasParser;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pipeline View UI")
class PipelineViewTest {

    private PipelineView createPipelineView() {
        ParserFactory parserFactory = new ParserFactory(
            new MTPAmlParser(), new UaServerDatasheetParser(),
            new OpcUaNodeSetParser(), new AasParser());
        UploadService uploadService = new UploadService(parserFactory);
        OntologyModelLoader ontologyModelLoader = new OntologyModelLoader();
        ontologyModelLoader.setFilePath("ontology/css-sda-unified.ttl");
        ontologyModelLoader.loadOntology();
        OntologyInferenceService inferenceService = new OntologyInferenceService(ontologyModelLoader);
        SparqlQueryLoader sparqlQueryLoader = new SparqlQueryLoader();
        RDeployService rDeployService = new RDeployService(ontologyModelLoader);
        SemanticReasonerService reasonerService = new SemanticReasonerService(
            ontologyModelLoader, sparqlQueryLoader, rDeployService);
        RequirementDiscoveryService discoveryService = new RequirementDiscoveryService(
            ontologyModelLoader, sparqlQueryLoader);
        EndToEndPipelineService pipelineService = new EndToEndPipelineService(
            uploadService, ontologyModelLoader, inferenceService, reasonerService);
        DockerComposeManifestService dockerComposeService = new DockerComposeManifestService();
        KubernetesManifestService kubernetesService = new KubernetesManifestService();
        return new PipelineView(pipelineService, dockerComposeService, kubernetesService, discoveryService);
    }

    @Test
    @DisplayName("Should be a VerticalLayout")
    void shouldBeVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(PipelineView.class));
    }

    @Test
    @DisplayName("Should have Route annotation with pipeline path")
    void shouldHaveRouteAnnotation() {
        Route route = PipelineView.class.getAnnotation(Route.class);
        assertNotNull(route, "PipelineView must have @Route annotation");
        assertEquals("pipeline", route.value());
    }

    @Test
    @DisplayName("Should have correct @PageTitle")
    void shouldHaveCorrectPageTitle() {
        PageTitle pageTitle = PipelineView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "PipelineView must have @PageTitle annotation");
        assertEquals("Semantic Reasoner", pageTitle.value());
    }

    @Test
    @DisplayName("Should create PipelineView with all dependencies")
    void shouldCreateViewWithDependencies() {
        PipelineView view = createPipelineView();
        assertNotNull(view);
    }

    @Test
    @DisplayName("Should hide predefined requirements REQ-001, REQ-002, REQ-003 from the filter")
    void shouldHidePredefinedRequirementsFromFilter() throws Exception {
        PipelineView view = createPipelineView();
        CheckboxGroup<String> filter = getRequirementFilter(view);

        List<String> items = filter.getListDataView().getItems().toList();
        assertFalse(items.contains("REQ-001"), "REQ-001 must be hidden from the filter");
        assertFalse(items.contains("REQ-002"), "REQ-002 must be hidden from the filter");
        assertFalse(items.contains("REQ-003"), "REQ-003 must be hidden from the filter");
    }

    @Test
    @DisplayName("Should return false when the requirement form is empty")
    void shouldRejectEmptyRequirementForm() {
        PipelineView view = createPipelineView();
        assertFalse(view.hasRequirementData(Map.of()));
    }

    @Test
    @DisplayName("Should return false when the title is missing")
    void shouldRejectRequirementWithoutTitle() {
        PipelineView view = createPipelineView();
        assertFalse(view.hasRequirementData(Map.of("description", "Monitor pump health")));
    }

    @Test
    @DisplayName("Should return false when the description is missing")
    void shouldRejectRequirementWithoutDescription() {
        PipelineView view = createPipelineView();
        assertFalse(view.hasRequirementData(Map.of("title", "Pump monitoring")));
    }

    @Test
    @DisplayName("Should accept a requirement with only the mandatory title and description")
    void shouldAcceptRequirementWithMandatoryFieldsOnly() {
        PipelineView view = createPipelineView();
        assertTrue(view.hasRequirementData(Map.of(
            "title", "Pump monitoring", "description", "Monitor pump health")));
    }

    @Test
    @DisplayName("Should ignore optional parameters when deciding on requirement data")
    void shouldIgnoreOptionalParameters() {
        PipelineView view = createPipelineView();
        assertFalse(view.hasRequirementData(Map.of(
            "protocol", "OPC-UA", "minRecall", "0.90", "maxLatency", "60")));
    }

    @Test
    @DisplayName("Should mark only the mandatory fields invalid when no requirement data is provided")
    void shouldMarkMandatoryFieldsInvalidWhenNoRequirementData() throws Exception {
        PipelineView view = createPipelineView();
        view.applyRequirementFormValidation(Map.of());

        TextField title = getField(view, "newReqTitle", TextField.class);
        TextArea description = getField(view, "newReqDescription", TextArea.class);
        ComboBox<String> protocol = getField(view, "newReqProtocol", ComboBox.class);
        assertTrue(title.isInvalid());
        assertEquals("Title is required", title.getErrorMessage());
        assertTrue(description.isInvalid());
        assertEquals("Description is required", description.getErrorMessage());
        assertFalse(protocol.isInvalid(), "Optional field Protocol must not be flagged as required");
    }

    @Test
    @DisplayName("Should clear form field errors when the mandatory fields are provided")
    void shouldClearFieldErrorsWhenMandatoryFieldsProvided() throws Exception {
        PipelineView view = createPipelineView();
        view.applyRequirementFormValidation(Map.of());
        view.applyRequirementFormValidation(Map.of(
            "title", "Pump monitoring", "description", "Monitor pump health"));

        TextField title = getField(view, "newReqTitle", TextField.class);
        TextArea description = getField(view, "newReqDescription", TextArea.class);
        assertFalse(title.isInvalid());
        assertFalse(description.isInvalid());
    }

    @Test
    @DisplayName("Should not flag a filled title and optional fields when the mandatory fields are provided")
    void shouldNotFlagOptionalFieldsWhenMandatoryProvided() throws Exception {
        PipelineView view = createPipelineView();
        TextField title = getField(view, "newReqTitle", TextField.class);
        title.setValue("Pump monitoring");
        view.applyRequirementFormValidation(Map.of(
            "title", "Pump monitoring", "description", "Monitor pump health"));

        assertFalse(title.isInvalid());
        ComboBox<String> protocol = getField(view, "newReqProtocol", ComboBox.class);
        assertFalse(protocol.isInvalid());
    }

    @Test
    @DisplayName("Should show required indicators on the mandatory fields only")
    void shouldShowRequiredIndicatorsOnMandatoryFieldsOnly() throws Exception {
        PipelineView view = createPipelineView();

        TextField title = getField(view, "newReqTitle", TextField.class);
        TextArea description = getField(view, "newReqDescription", TextArea.class);
        ComboBox<String> protocol = getField(view, "newReqProtocol", ComboBox.class);
        assertTrue(title.isRequiredIndicatorVisible(), "Title is mandatory");
        assertTrue(description.isRequiredIndicatorVisible(), "Description is mandatory");
        assertFalse(protocol.isRequiredIndicatorVisible(), "Protocol is optional");
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(PipelineView view, String name, Class<T> type) throws Exception {
        java.lang.reflect.Field field = PipelineView.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(view));
    }

    @SuppressWarnings("unchecked")
    private static CheckboxGroup<String> getRequirementFilter(PipelineView view) throws Exception {
        java.lang.reflect.Field field = PipelineView.class.getDeclaredField("requirementFilter");
        field.setAccessible(true);
        return (CheckboxGroup<String>) field.get(view);
    }
}
