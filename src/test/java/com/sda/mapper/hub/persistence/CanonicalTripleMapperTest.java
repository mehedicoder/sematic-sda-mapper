package com.sda.mapper.hub.persistence;

import com.sda.mapper.hub.model.Asset;
import com.sda.mapper.hub.model.EntityOrigin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Canonical triple mapper")
class CanonicalTripleMapperTest {

    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";

    @Test
    @DisplayName("Should map canonical entity to CSS-SDA instance triples")
    void shouldMapCanonicalEntityToInstanceTriples() {
        CanonicalTripleMapper mapper = new CanonicalTripleMapper();
        UUID sessionId = UUID.randomUUID();
        Asset asset = new Asset("asset-1", "Pump FT101", "ACME", "SN-1", "pump", "pump-aas.json");

        List<InstanceTriple> triples = mapper.toTriples(sessionId, null, UUID.randomUUID(),
            "INSTANTIATE", EntityOrigin.PARSED, false, List.of(asset));

        assertTrue(triples.stream().anyMatch(t -> t.predicateIri().equals(RDF_TYPE)
            && t.objectValue().equals("http://sda-platform.org/ontology#Asset")
            && !t.objectIsLiteral()));
        assertTrue(triples.stream().anyMatch(t -> t.predicateIri().equals(RDFS_LABEL)
            && t.objectValue().equals("Pump FT101")
            && t.objectIsLiteral()));
        assertTrue(triples.stream().anyMatch(t -> t.predicateIri().equals("http://sda-platform.org/ontology#manufacturer")
            && t.objectValue().equals("ACME")));
        assertEquals(sessionId, triples.get(0).sessionId());
        assertTrue(triples.stream().allMatch(t -> t.origin() == EntityOrigin.PARSED));
    }
}
