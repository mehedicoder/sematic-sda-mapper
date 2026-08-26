package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.SkillParameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MTP model parser")
class MtpModelParserTest {

    private final MtpModelParser parser = new MtpModelParser();

    private InputStream fixture() {
        InputStream stream = getClass().getResourceAsStream("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-module-mtp.xml");
        if (stream == null) {
            throw new IllegalStateException("Fixture pump-module-mtp.xml not found");
        }
        return stream;
    }

    @Test
    @DisplayName("Should report the handled format")
    void shouldHandleMtp() {
        assertEquals(AssetFileFormat.MTP_AML, parser.format());
        assertTrue(parser.supports(AssetFileFormat.MTP_AML));
    }

    @Test
    @DisplayName("Should parse the MTP pump fixture into skills, parameters, binding and nodes")
    void shouldParseMtpFixture() throws Exception {
        CanonicalAssetModel model;
        try (InputStream in = fixture()) {
            model = parser.parse(in, "pump-module-mtp.xml");
        }
        assertEquals(2, model.skills().size());
        assertTrue(model.skills().stream().anyMatch(s -> s.name().equals("Pumping")));
        assertEquals(5, model.skillParameters().size());
        SkillParameter flow = model.skillParameters().stream()
            .filter(p -> p.name().equals("FlowRate")).findFirst().orElseThrow();
        assertEquals("l/min", flow.engineeringUnit());
        assertEquals("FlowControl", flow.functionClass());
        assertEquals(1, model.communicationBindings().size());
        CommunicationBinding binding = model.communicationBindings().get(0);
        assertEquals("opc.tcp://pump01:4840", binding.endpointUrl());
        assertEquals("SignAndEncrypt", binding.securityMode());
        assertEquals(4, model.processVariables().size());
        assertTrue(model.processVariables().stream()
            .anyMatch(v -> v.name().equals("Temperature")));
    }

    @Test
    @DisplayName("Should reject a non-MTP XML root element")
    void shouldRejectWrongRoot() {
        ParsingException exception = assertThrows(ParsingException.class, () ->
            parser.parse(new java.io.ByteArrayInputStream(
                "<NotMtpAml/>".getBytes(StandardCharsets.UTF_8)), "wrong.xml"));
        assertTrue(exception.getMessage().contains("wrong.xml"));
    }

    @Test
    @DisplayName("Should reject malformed XML")
    void shouldRejectMalformedXml() {
        assertThrows(ParsingException.class, () -> parser.parse(
            new java.io.ByteArrayInputStream(
                "<MtpAml><broken>".getBytes(StandardCharsets.UTF_8)), "broken.xml"));
    }

    @Test
    @DisplayName("Should reject an MTP document without content")
    void shouldRejectEmptyMtp() {
        assertThrows(ParsingException.class, () -> parser.parse(
            new java.io.ByteArrayInputStream(
                "<MtpAml></MtpAml>".getBytes(StandardCharsets.UTF_8)), "empty.xml"));
    }
}
