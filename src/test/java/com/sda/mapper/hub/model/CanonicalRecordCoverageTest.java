package com.sda.mapper.hub.model;

import com.sda.mapper.model.OntologyClass;
import com.sda.mapper.model.OntologyDatatypeProperty;
import com.sda.mapper.service.OntologyDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Canonical record coverage of the v2 ontology catalog")
class CanonicalRecordCoverageTest {

    @Test
    @DisplayName("Should expose a typed canonical record for every numbered ontology class")
    void shouldHaveRecordForEveryNumberedClass() {
        OntologyDataService ontology = new OntologyDataService();
        for (OntologyClass clazz : ontology.getClasses()) {
            try {
                Class.forName("com.sda.mapper.hub.model." + clazz.name());
            } catch (ClassNotFoundException e) {
                fail("No canonical record for numbered class: " + clazz.name());
            }
        }
    }

    @Test
    @DisplayName("Should keep every derived subclass parented to a numbered class")
    void shouldParentDerivedClassesToNumberedParents() {
        OntologyDataService ontology = new OntologyDataService();
        List<String> numberedNames = ontology.getClasses().stream()
            .map(OntologyClass::name)
            .toList();
        for (OntologyClass subClass : ontology.getSubClasses()) {
            assertTrue(numberedNames.contains(subClass.parent()),
                "Derived class " + subClass.name() + " must be parented to a numbered class");
        }
    }

    @Test
    @DisplayName("Should expose all 155 datatype properties across the 24 numbered classes")
    void shouldExposeDatatypeProperties() {
        OntologyDataService ontology = new OntologyDataService();
        List<OntologyDatatypeProperty> properties = ontology.getDatatypeProperties();
        assertEquals(155, properties.size());

        List<String> stakeholder = ontology.getDatatypeProperties("Stakeholder").stream()
            .map(OntologyDatatypeProperty::name)
            .toList();
        assertEquals(List.of("contactInfo", "organization", "role", "stakeholderID", "stakeholderName"),
            stakeholder);
        assertTrue(ontology.getDatatypeProperties("Stakeholder").stream()
            .anyMatch(p -> p.name().equals("stakeholderName") && "xsd:string".equals(p.xsdType())));
    }
}
