package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CapabilityMatch;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.DeploymentContext;
import com.sda.mapper.hub.model.OfferedCapability;
import com.sda.mapper.hub.model.RequiredCapability;
import com.sda.mapper.hub.model.Service;
import com.sda.mapper.hub.model.Skill;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Semantic SPARQL match stage")
class SemanticMatchServiceTest {

    private final SemanticMatchService service = new SemanticMatchService(new SparqlQueryLoader());

    private CanonicalAssetModel demoModel(String networkZone) {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Skill("PUMP-001.Pumping", "Pumping", "Primary pumping", null));
        model.addEntity(new Service("SVC-PumpModule-FT101", "PumpModule-FT101 Service",
            "Exposed by AAS PumpModule-FT101", "OPC-UA", "pump-aas.json"));
        model.addEntity(new CommunicationBinding("opc.tcp://pump01:4840", "PumpModule-FT101 OPC UA binding",
            "OPC-UA", "opc.tcp://pump01:4840", "SignAndEncrypt", "Basic256Sha256", "pump.xml"));
        model.addEntity(new DeploymentContext("DC-opc.tcp://pump01:4840", "PumpModule-FT101 context",
            "Inferred from opc.tcp://pump01:4840", "edge", networkZone, "pump.xml"));
        return model;
    }

    private List<com.sda.mapper.hub.model.CanonicalEntity> offeredFlowControl() {
        return List.of(new OfferedCapability("OC-PUMP-001.Pumping", "PUMP-001.Pumping", "Pumping",
            "FlowControl", "l/min", "0", "100", "", "3", "pump.xml"));
    }

    @Test
    @DisplayName("Should match a required capability against an offered capability across all dimensions")
    void shouldMatchAcrossAllDimensions() {
        CanonicalAssetModel model = demoModel("OT");
        model.addEntity(new RequiredCapability("RC-1", "Required flow control", "FlowControl",
            "l/min", "0", "100", "2", "OPC-UA", "OT"));

        List<CapabilityMatch> matches = service.match(model, offeredFlowControl());

        assertEquals(1, matches.size());
        CapabilityMatch match = matches.get(0);
        assertEquals("RC-1", match.requiredCapabilityId());
        assertEquals("OC-PUMP-001.Pumping", match.offeredCapabilityId());
        assertEquals("PUMP-001.Pumping", match.skillId());
        assertEquals("PumpModule-FT101 Service", match.serviceName());
        assertEquals("opc.tcp://pump01:4840", match.endpointUrl());
        assertEquals("OPC-UA", match.protocol());
        assertEquals("OT", match.networkZone());
        assertEquals("3", match.safetyIntegrityLevel());
        assertTrue(match.inferred());
    }

    @Test
    @DisplayName("Should not match when the function classes differ")
    void shouldNotMatchDifferentFunctionClass() {
        CanonicalAssetModel model = demoModel("OT");
        model.addEntity(new RequiredCapability("RC-2", "Required temperature control", "TemperatureControl",
            "l/min", "0", "100", "", "OPC-UA", "OT"));

        assertTrue(service.match(model, offeredFlowControl()).isEmpty());
    }

    @Test
    @DisplayName("Should not match when the offered nominal range does not contain the required range")
    void shouldNotMatchWhenRangeNotContained() {
        CanonicalAssetModel model = demoModel("OT");
        model.addEntity(new RequiredCapability("RC-3", "High range flow control", "FlowControl",
            "l/min", "50", "200", "", "OPC-UA", "OT"));

        assertTrue(service.match(model, offeredFlowControl()).isEmpty());
    }

    @Test
    @DisplayName("Should not match when the engineering units differ")
    void shouldNotMatchDifferentUnit() {
        CanonicalAssetModel model = demoModel("OT");
        model.addEntity(new RequiredCapability("RC-4", "Flow control in m3/h", "FlowControl",
            "m\u00B3/h", "0", "100", "", "OPC-UA", "OT"));

        assertTrue(service.match(model, offeredFlowControl()).isEmpty());
    }

    @Test
    @DisplayName("Should not match when the offered SIL is below the required SIL")
    void shouldNotMatchInsufficientSil() {
        CanonicalAssetModel model = demoModel("OT");
        model.addEntity(new RequiredCapability("RC-5", "SIL3 flow control", "FlowControl",
            "l/min", "0", "100", "3", "OPC-UA", "OT"));
        List<com.sda.mapper.hub.model.CanonicalEntity> offeredSil2 = List.of(
            new OfferedCapability("OC-PUMP-001.Pumping", "PUMP-001.Pumping", "Pumping",
                "FlowControl", "l/min", "0", "100", "", "2", "pump.xml"));

        assertTrue(service.match(model, offeredSil2).isEmpty());
    }

    @Test
    @DisplayName("Should not match when the binding protocol differs from the required protocol")
    void shouldNotMatchDifferentProtocol() {
        CanonicalAssetModel model = demoModel("OT");
        model.addEntity(new RequiredCapability("RC-6", "Modbus flow control", "FlowControl",
            "l/min", "0", "100", "", "MODBUS", "OT"));

        assertTrue(service.match(model, offeredFlowControl()).isEmpty());
    }

    @Test
    @DisplayName("Should not match when the deployment network zone differs from the required zone")
    void shouldNotMatchDifferentZone() {
        CanonicalAssetModel model = demoModel("IT");
        model.addEntity(new RequiredCapability("RC-7", "OT zone flow control", "FlowControl",
            "l/min", "0", "100", "", "OPC-UA", "OT"));

        assertTrue(service.match(model, offeredFlowControl()).isEmpty());
    }

    @Test
    @DisplayName("Should match even when the requirement omits optional dimensions")
    void shouldMatchOnCapabilityOnly() {
        CanonicalAssetModel model = demoModel("OT");
        model.addEntity(new RequiredCapability("RC-8", "Plain flow control", "FlowControl",
            "l/min", "", "", "", "", ""));

        List<CapabilityMatch> matches = service.match(model, offeredFlowControl());

        assertEquals(1, matches.size());
        assertEquals("RC-8", matches.get(0).requiredCapabilityId());
    }

    @Test
    @DisplayName("Should return no matches when the model has no required capabilities")
    void shouldReturnNoMatchesWithoutRequirements() {
        CanonicalAssetModel model = demoModel("OT");

        assertTrue(service.match(model, offeredFlowControl()).isEmpty());
    }
}
