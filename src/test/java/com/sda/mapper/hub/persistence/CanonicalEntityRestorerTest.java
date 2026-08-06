package com.sda.mapper.hub.persistence;

import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.Sensor;
import com.sda.mapper.hub.model.ServiceInstance;
import com.sda.mapper.hub.model.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Canonical entity restorer")
class CanonicalEntityRestorerTest {

    private final CanonicalEntityRestorer restorer = new CanonicalEntityRestorer();

    @Test
    @DisplayName("Should rebuild a typed Skill record from persisted attributes")
    void shouldRebuildSkill() {
        Map<String, Object> attributes = Map.of(
            "id", "SK-1", "name", "Conveyor control", "description", "Runs conveyor",
            "capabilityId", "CAP-9");

        CanonicalEntity entity = restorer.restore("Skill", "SK-1", "Conveyor control",
            "pump-aas.json", false, attributes);

        assertTrue(entity instanceof Skill);
        Skill skill = (Skill) entity;
        assertEquals("SK-1", skill.id());
        assertEquals("Conveyor control", skill.name());
        assertEquals("Runs conveyor", skill.description());
        assertEquals("CAP-9", skill.capabilityId());
        assertEquals("pump-aas.json", skill.sourceFileName().orElseThrow());
        assertFalse(skill.inferred());
    }

    @Test
    @DisplayName("Should mark restored entities as inferred from the persisted flag")
    void shouldRebuildInferredFlag() {
        CanonicalEntity entity = restorer.restore("Sensor", "S-1", "Temperature", null,
            true, Map.of("assetId", "A-1", "unit", "degC"));

        assertTrue(entity instanceof Sensor);
        Sensor sensor = (Sensor) entity;
        assertTrue(sensor.inferred());
        assertTrue(sensor.sourceFileName().isEmpty());
        assertEquals("A-1", sensor.assetId());
        assertEquals("degC", sensor.unit());
    }

    @Test
    @DisplayName("Should restore entities whose attribute keys differ from component names")
    void shouldRebuildServiceInstance() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("id", "SI-1");
        attributes.put("serviceId", "SRV-2");
        attributes.put("endpointUrl", "opc.tcp://localhost:4840");

        CanonicalEntity entity = restorer.restore("ServiceInstance", "SI-1", "Instance",
            null, false, attributes);

        assertTrue(entity instanceof ServiceInstance);
        ServiceInstance instance = (ServiceInstance) entity;
        assertEquals("SRV-2", instance.serviceId());
        assertEquals("opc.tcp://localhost:4840", instance.endpointUrl());
    }

    @Test
    @DisplayName("Should reject unknown ontology classes")
    void shouldRejectUnknownClass() {
        assertThrows(IllegalArgumentException.class, () ->
            restorer.restore("UnknownClass", "X-1", "X", null, false, Map.of()));
    }
}
