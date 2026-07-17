package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.parser.MTPAmlParser;
import com.sda.mapper.parser.ParseResult;
import com.sda.mapper.parser.UaServerDatasheetParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("End-to-End Pipeline Service")
class EndToEndPipelineServiceTest {

    private final MTPAmlParser mtpParser = new MTPAmlParser();
    private final UaServerDatasheetParser uaParser = new UaServerDatasheetParser();

    @Test
    @DisplayName("MTP AML parse should produce Jena Model with Skills and Bindings")
    void shouldParseMtpAmlIntoModel() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/mtp-fermentation-skid.xml")) {
            assertNotNull(input);
            ParseResult result = mtpParser.parse(input);
            assertNotNull(result.model(), "Model should not be null");
            assertTrue(result.model().size() > 0, "Model should contain triples");

            long skillCount = result.model().listSubjectsWithProperty(
                result.model().createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                result.model().createResource("http://www.example.org/sda#Skill")
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
                .getResourceAsStream("examples/uaserver-ahu01.json")) {
            assertNotNull(input);
            ParseResult result = uaParser.parse(input);
            assertNotNull(result.model(), "Model should not be null");
            assertTrue(result.model().size() > 0, "Model should contain triples");

            long connectorCount = result.model().listSubjectsWithProperty(
                result.model().createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                result.model().createResource("http://www.example.org/sda#DataConnector")
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
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();

        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/mtp-fermentation-skid.xml")) {
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
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        var queryLoader = new SparqlQueryLoader();
        var reasoner = new SemanticReasonerService(loader, queryLoader);

        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertTrue(result.matched());
        assertNotNull(result.connector(), "connector should be populated");
        assertNotNull(result.deploymentContext(), "deploymentContext should be populated");
        assertFalse(result.connector().isBlank(), "connector should not be blank");
        assertFalse(result.deploymentContext().isBlank(), "deploymentContext should not be blank");
    }
}
