package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.parser.AasParser;
import com.sda.mapper.parser.MTPAmlParser;
import com.sda.mapper.parser.OpcUaNodeSetParser;
import com.sda.mapper.parser.ParserFactory;
import com.sda.mapper.parser.ParseResult;
import com.sda.mapper.parser.UaServerDatasheetParser;
import com.sda.mapper.parser.UploadService;
import com.sda.mapper.support.DemoAboxBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("End-to-End Pipeline Service")
class EndToEndPipelineServiceTest {

    private final MTPAmlParser mtpParser = new MTPAmlParser();
    private final UaServerDatasheetParser uaParser = new UaServerDatasheetParser();

    private EndToEndPipelineService createPipelineService() {
        ParserFactory parserFactory = new ParserFactory(
            new MTPAmlParser(), new UaServerDatasheetParser(),
            new OpcUaNodeSetParser(), new AasParser());
        UploadService uploadService = new UploadService(parserFactory);
        OntologyModelLoader ontologyModelLoader = new OntologyModelLoader();
        ontologyModelLoader.setFilePath("ontology/sda_ontology_v2_structured.ttl");
        ontologyModelLoader.loadOntology();
        DemoAboxBuilder.apply(ontologyModelLoader.getModel());
        OntologyInferenceService inferenceService = new OntologyInferenceService(ontologyModelLoader);
        SparqlQueryLoader sparqlQueryLoader = new SparqlQueryLoader();
        RDeployService rDeployService = new RDeployService(ontologyModelLoader);
        SemanticReasonerService reasonerService = new SemanticReasonerService(
            ontologyModelLoader, sparqlQueryLoader, rDeployService);
        return new EndToEndPipelineService(
            uploadService, ontologyModelLoader, inferenceService, reasonerService);
    }

    @Test
    @DisplayName("Explicit empty requirement list should not auto-discover all requirements")
    void shouldNotAutoDiscoverWithExplicitEmptyList() {
        EndToEndPipelineService pipelineService = createPipelineService();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input);
            EndToEndPipelineService.PipelineResult result =
                pipelineService.execute("req002-mtp-fermentation-skid.xml", input, List.of(), null);
            assertTrue(result.matches().isEmpty(),
                "Auto discovery must be disabled for an explicit empty list, got: " + result.matches().keySet());
            assertTrue(result.warnings().stream().anyMatch(w -> w.contains("No requirements selected")),
                "Should warn that no requirements were selected");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Null requirement list should not auto-discover all requirements")
    void shouldNotAutoDiscoverWithNullList() {
        EndToEndPipelineService pipelineService = createPipelineService();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input);
            EndToEndPipelineService.PipelineResult result =
                pipelineService.execute("req002-mtp-fermentation-skid.xml", input, null, null);
            assertTrue(result.matches().isEmpty(),
                "Auto discovery must be disabled for a null list, got: " + result.matches().keySet());
            assertTrue(result.warnings().stream().anyMatch(w -> w.contains("No requirements selected")),
                "Should warn that no requirements were selected");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("UseCaseRequirement is created from form params even when no requirement list is selected")
    void shouldCreateRequirementFromFormParams() {
        EndToEndPipelineService pipelineService = createPipelineService();
        Map<String, String> formParams = new HashMap<>();
        formParams.put("title", "Test Flow Rate UseCaseRequirement");
        formParams.put("protocol", "OPC-UA");
        formParams.put("accessMode", "read-only");
        formParams.put("minRecall", "0.90");
        formParams.put("maxLatency", "60");
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input);
            EndToEndPipelineService.PipelineResult result =
                pipelineService.execute("req002-mtp-fermentation-skid.xml", input, List.of(), formParams);
            assertFalse(result.matches().isEmpty(),
                "A form-created requirement should be matched");
            assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Created new requirement")),
                "Should warn about the created requirement");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("MTP AML parse should produce Jena Model with Skills and Bindings")
    void shouldParseMtpAmlIntoModel() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input);
            ParseResult result = mtpParser.parse(input);
            assertNotNull(result.model(), "Model should not be null");
            assertTrue(result.model().size() > 0, "Model should contain triples");

            long skillCount = result.model().listSubjectsWithProperty(
                result.model().createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                result.model().createResource("http://sda-platform.org/ontology#Skill")
            ).toList().size();
            assertTrue(skillCount > 0, "Model should contain Skill instances");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("OPC UA datasheet parse should produce Jena Model with Connectors")
    void shouldParseUaDatasheetIntoModel() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req003-uaserver-ahu01.json")) {
            assertNotNull(input);
            ParseResult result = uaParser.parse(input);
            assertNotNull(result.model(), "Model should not be null");
            assertTrue(result.model().size() > 0, "Model should contain triples");

            long connectorCount = result.model().listSubjectsWithProperty(
                result.model().createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                result.model().createResource("http://sda-platform.org/ontology#DataConnector")
            ).toList().size();
            assertTrue(connectorCount > 0, "Model should contain DataConnector instances");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Parsed Model can be merged into base ontology and queried")
    void shouldMergeParsedModelAndQuery() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/sda_ontology_v2_structured.ttl");
        loader.loadOntology();

        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid.xml")) {
            assertNotNull(input);
            ParseResult result = mtpParser.parse(input);

            long baseSize = loader.getModel().size();
            loader.getModel().add(result.model());
            assertTrue(loader.getModel().size() > baseSize,
                "Base model should grow after merging parsed model");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Match result should include connector and deploymentContext labels")
    void shouldReturnConnectorAndDeploymentContext() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/sda_ontology_v2_structured.ttl");
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
        var queryLoader = new SparqlQueryLoader();
        var rDeployService = new RDeployService(loader);
        var reasoner = new SemanticReasonerService(loader, queryLoader, rDeployService);

        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertTrue(result.matched());
        assertNotNull(result.connector(), "connector should be populated");
        assertNotNull(result.deploymentContext(), "deploymentContext should be populated");
        assertFalse(result.connector().isBlank(), "connector should not be blank");
        assertFalse(result.deploymentContext().isBlank(), "deploymentContext should not be blank");
    }
}
