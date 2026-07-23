package com.sda.mapper.ui;

import com.sda.mapper.deployment.DockerComposeManifestService;
import com.sda.mapper.deployment.KubernetesManifestService;
import com.sda.mapper.reasoning.EndToEndPipelineService;
import com.sda.mapper.reasoning.OntologyInferenceService;
import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.RequirementDiscoveryService;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.ontology.OntologyConflictDetector;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.ontology.OntologyPersistenceService;
import com.sda.mapper.ontology.ServiceRegistrationService;
import com.sda.mapper.parser.UploadService;
import com.sda.mapper.parser.ParserFactory;
import com.sda.mapper.parser.MTPAmlParser;
import com.sda.mapper.parser.UaServerDatasheetParser;
import com.sda.mapper.parser.OpcUaNodeSetParser;
import com.sda.mapper.parser.AasParser;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pipeline View UI")
class PipelineViewTest {

    private PipelineView createPipelineView() {
        ParserFactory parserFactory = new ParserFactory(
            new MTPAmlParser(), new UaServerDatasheetParser(),
            new OpcUaNodeSetParser(), new AasParser());
        UploadService uploadService = new UploadService(parserFactory);
        OntologyModelLoader ontologyModelLoader = new OntologyModelLoader();
        ontologyModelLoader.setFilePath("ontology/css-sda-small.ttl");
        ontologyModelLoader.loadOntology();
        OntologyInferenceService inferenceService = new OntologyInferenceService(ontologyModelLoader);
        SparqlQueryLoader sparqlQueryLoader = new SparqlQueryLoader();
        RDeployService rDeployService = new RDeployService(ontologyModelLoader);
        SemanticReasonerService reasonerService = new SemanticReasonerService(
            ontologyModelLoader, sparqlQueryLoader, rDeployService);
        RequirementDiscoveryService discoveryService = new RequirementDiscoveryService(
            ontologyModelLoader, sparqlQueryLoader);
        OntologyPersistenceService persistenceService = new OntologyPersistenceService();
        OntologyConflictDetector conflictDetector = new OntologyConflictDetector();
        ServiceRegistrationService registrationService = new ServiceRegistrationService(
            conflictDetector, persistenceService, ontologyModelLoader);
        EndToEndPipelineService pipelineService = new EndToEndPipelineService(
            uploadService, ontologyModelLoader, inferenceService, reasonerService, discoveryService,
            registrationService);
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
}
