package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.ProcessVariable;
import com.sda.mapper.hub.model.Skill;
import com.sda.mapper.hub.model.SkillParameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Inference stage service")
class InferenceServiceTest {

    private final InferenceService service = new InferenceService();

    @Test
    @DisplayName("Should derive one sensor per measured process variable")
    void shouldDeriveSensorPerVariable() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new ProcessVariable("flow", "FlowRate", "l/min", "ns=1;s=Flow", "Pump01"));

        List<CanonicalEntity> inferred = service.infer(model);

        List<CanonicalEntity> sensors = inferred.stream()
            .filter(e -> e.className().equals("Sensor")).toList();
        assertEquals(1, sensors.size());
        assertTrue(sensors.get(0).name().contains("FlowRate"));
        assertTrue(sensors.get(0).attributes().get("unit").equals("l/min"));
    }

    @Test
    @DisplayName("Should derive one capability per skill")
    void shouldDeriveCapabilityPerSkill() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Skill("PUMP-001", "Pumping", "Primary pumping", null));

        List<CanonicalEntity> inferred = service.infer(model);

        List<CanonicalEntity> capabilities = inferred.stream()
            .filter(e -> e.className().equals("Capability")).toList();
        assertEquals(1, capabilities.size());
        assertTrue(capabilities.get(0).name().contains("Pumping"));
    }

    @Test
    @DisplayName("Should derive instance, cluster node, network zone and context per binding")
    void shouldDeriveDeploymentEntitiesPerBinding() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new CommunicationBinding("cb-1", "OPC UA binding", "OPC-UA",
            "opc.tcp://pump01:4840", "None", ""));

        List<CanonicalEntity> inferred = service.infer(model);

        assertEquals(1, inferred.stream().filter(e -> e.className().equals("ServiceInstance")).count());
        assertEquals(1, inferred.stream().filter(e -> e.className().equals("ClusterNode")).count());
        assertEquals(1, inferred.stream().filter(e -> e.className().equals("NetworkZone")).count());
        assertEquals(1, inferred.stream().filter(e -> e.className().equals("DeploymentContext")).count());
        assertTrue(inferred.stream().filter(e -> e.className().equals("ServiceInstance"))
            .allMatch(e -> e.attributes().get("endpointUrl").equals("opc.tcp://pump01:4840")));
    }

    @Test
    @DisplayName("Should mark all inferred entities as inferred")
    void shouldMarkInferredEntities() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new ProcessVariable("flow", "FlowRate", "l/min", "ns=1;s=Flow", "Pump01"));
        model.addEntity(new Skill("PUMP-001", "Pumping", "Primary pumping", null));
        model.addEntity(new CommunicationBinding("cb-1", "OPC UA binding", "OPC-UA",
            "opc.tcp://pump01:4840", "None", ""));

        List<CanonicalEntity> inferred = service.infer(model);

        assertEquals(6, inferred.size());
        assertTrue(inferred.stream().allMatch(CanonicalEntity::inferred));
    }

    @Test
    @DisplayName("Should derive one offered capability per skill from parameter semantics")
    void shouldDeriveOfferedCapabilityFromParameter() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Skill("PUMP-001", "Pumping", "Primary pumping", null));
        model.addEntity(new SkillParameter("PUMP-001.FlowRate", "PUMP-001", "FlowRate",
            "FlowControl", "l/min", "0", "100", "Double", "pump.xml"));

        List<CanonicalEntity> inferred = service.infer(model);

        List<CanonicalEntity> offered = inferred.stream()
            .filter(e -> e.className().equals("OfferedCapability")).toList();
        assertEquals(1, offered.size());
        assertTrue(offered.get(0).attributes().get("functionClass").equals("FlowControl"));
        assertTrue(offered.get(0).attributes().get("engineeringUnit").equals("l/min"));
        assertTrue(offered.get(0).attributes().get("nominalRangeMin").equals("0"));
        assertTrue(offered.get(0).attributes().get("nominalRangeMax").equals("100"));
        assertTrue(offered.get(0).attributes().get("skillId").equals("PUMP-001"));
        assertTrue(offered.get(0).inferred());
    }

    @Test
    @DisplayName("Should not derive an offered capability for a skill without parameters")
    void shouldNotDeriveOfferedCapabilityWithoutParameter() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Skill("PUMP-002", "Standby", "No parameters", null));

        List<CanonicalEntity> inferred = service.infer(model);

        assertEquals(0, inferred.stream()
            .filter(e -> e.className().equals("OfferedCapability")).count());
    }

    @Test
    @DisplayName("Should prefer the parameter carrying a functionClass for capability inference")
    void shouldPreferFunctionClassParameter() {
        CanonicalAssetModel model = new CanonicalAssetModel();
        model.addEntity(new Skill("SK-1", "Dosing", "Dosing skill", null));
        model.addEntity(new SkillParameter("SK-1.Rate", "SK-1", "Rate",
            "FlowControl", "l/min", "0", "50", "Double", "d.xml"));
        model.addEntity(new SkillParameter("SK-1.Temp", "SK-1", "Temp",
            null, "degC", "10", "80", "Double", "d.xml"));

        List<CanonicalEntity> inferred = service.infer(model);

        CanonicalEntity offered = inferred.stream()
            .filter(e -> e.className().equals("OfferedCapability")).findFirst().orElseThrow();
        assertEquals("FlowControl", offered.attributes().get("functionClass"));
        assertEquals("l/min", offered.attributes().get("engineeringUnit"));
    }

    @Test
    @DisplayName("Should extract host names from endpoint URLs")
    void shouldExtractHostFromEndpoint() {
        assertEquals("pump01", InferenceService.endpointHost("opc.tcp://pump01:4840"));
        assertEquals("pump01", InferenceService.endpointHost("pump01:4840"));
        assertEquals("pump01", InferenceService.endpointHost("opc.tcp://pump01"));
        assertEquals("unknown", InferenceService.endpointHost(""));
        assertEquals("unknown", InferenceService.endpointHost(null));
    }
}
