package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.CanonicalAssetModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Requirement TTL Parser")
class RequirementTtlParserTest {

    private static final String REQUIREMENT_TTL = """
        @prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
        @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
        @prefix sda:  <http://sda-platform.org/ontology#> .
        @prefix :     <http://sda-platform.org/instance#> .

        :REQ-MY-001 a sda:UseCaseRequirement ;
            rdfs:label "Flowmeter Diagnostics Requirement" ;
            sda:requirementID "REQ-MY-001" ;
            sda:requirementName "Flowmeter Diagnostics Requirement" ;
            sda:requirementDescription "Match a flowmeter diagnostics service." ;
            sda:mapsToCapability :CAP-MY-FlowDiagnostics .

        :CAP-MY-FlowDiagnostics a sda:RequiredCapability ;
            rdfs:label "Flowmeter Diagnostics (Required)" ;
            sda:capabilityID "CAP-MY-FLOW-DIAG-001" ;
            sda:capabilityName "Flowmeter Diagnostics" ;
            sda:capabilityType "FlowDiagnostics" .
        """;

    private RequirementTtlParser parser() {
        return new RequirementTtlParser();
    }

    @Test
    @DisplayName("Should declare the REQUIREMENT_TTL format")
    void shouldDeclareFormat() {
        assertEquals(com.sda.mapper.hub.model.AssetFileFormat.REQUIREMENT_TTL, parser().format());
    }

    @Test
    @DisplayName("Should parse a UseCaseRequirement linked to a RequiredCapability with capabilityType")
    void shouldParseRequirementAndRequiredCapability() {
        CanonicalAssetModel model = parser().parse(stream(REQUIREMENT_TTL), "demo.ttl");

        assertEquals(1, model.requirements().size(), "one use case requirement expected");
        assertEquals("REQ-MY-001", model.requirements().get(0).id());
        assertEquals("Flowmeter Diagnostics Requirement", model.requirements().get(0).name());

        assertEquals(1, model.requiredCapabilities().size(), "one required capability expected");
        var rc = model.requiredCapabilities().get(0);
        assertEquals("CAP-MY-FLOW-DIAG-001", rc.id());
        assertEquals("FlowDiagnostics", rc.functionClass(),
            "capabilityType must be carried as functionClass for catalogue matching");
        assertTrue(rc.sourceFileName().filter(n -> n.contains("demo.ttl")).isPresent(),
            "provenance must record the source file");
    }

    @Test
    @DisplayName("Should return an empty model when no requirement is present")
    void shouldReturnEmptyModelForNonRequirementTtl() {
        CanonicalAssetModel model = parser().parse(
            stream("@prefix sda: <http://sda-platform.org/ontology#> ."), "empty.ttl");
        assertTrue(model.requirements().isEmpty());
        assertTrue(model.requiredCapabilities().isEmpty());
    }

    /** Small adapter so the test reads a String as an InputStream. */
    private static java.io.ByteArrayInputStream stream(String text) {
        return new java.io.ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
