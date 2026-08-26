package com.sda.mapper.hub.service;

import com.sda.mapper.llm.ConceptHint;
import com.sda.mapper.hub.model.ManualEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConceptHintMapper should normalise LLM hints into manual entities")
class ConceptHintMapperTest {

    @Test
    @DisplayName("Should keep supported classes and skip unsupported classes with a warning")
    void shouldFilterUnsupportedClasses() {
        List<ConceptHint> hints = List.of(
            new ConceptHint("RequiredCapability", "FlowControl demand", "pump needs flow", "FlowControl"),
            new ConceptHint("CommunicationBinding", "OPC-UA", "uses opcua", null),
            new ConceptHint("Gibberish", "nope", "unknown", null));

        List<ConceptHint> supported = ConceptHintMapper.filterSupported(hints);

        assertThat(supported).hasSize(1);
        assertThat(supported.get(0).className()).isEqualTo("RequiredCapability");
    }

    @Test
    @DisplayName("Should map a RequiredCapability hint using its capabilityType as functionClass")
    void shouldMapRequiredCapabilityWithCapabilityType() {
        ConceptHint hint = new ConceptHint("RequiredCapability", "FlowControl demand",
            "pump needs flow control", "FlowControl");

        ManualEntity entity = ConceptHintMapper.toManualEntity(hint);

        assertThat(entity.className()).isEqualTo("RequiredCapability");
        assertThat(entity.name()).isEqualTo("FlowControl demand");
        assertThat(entity.attributes().get("functionClass")).isEqualTo("FlowControl");
    }

    @Test
    @DisplayName("Should fall back to the label when capabilityType is missing for a RequiredCapability")
    void shouldFallbackWhenCapabilityTypeMissing() {
        ConceptHint hint = new ConceptHint("RequiredCapability", "FlowControl demand",
            "pump needs flow control", null);

        ManualEntity entity = ConceptHintMapper.toManualEntity(hint);

        assertThat(entity.attributes().get("functionClass")).isEqualTo("FlowControl demand");
    }

    @Test
    @DisplayName("Should reject an unsupported hint when building a manual entity")
    void shouldRejectUnsupportedHint() {
        ConceptHint hint = new ConceptHint("CommunicationBinding", "OPC-UA", "uses opcua", null);

        assertThatThrownBy(() -> ConceptHintMapper.toManualEntity(hint))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
