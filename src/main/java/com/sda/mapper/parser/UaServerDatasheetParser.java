package com.sda.mapper.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sda.mapper.error.ParserException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses IDTA 02009-1-0 OPC UA Server Datasheet JSON files into SDA semantic triples.
 * Extracts CommunicationBindings from EndpointDescriptions, DataConnectors from
 * Configuration/NodeSets, and device metadata from Identification.
 * Produces a Jena Model containing instantiated ABox triples.
 */
@Component
public class UaServerDatasheetParser implements Parser {

    private static final Logger log = LoggerFactory.getLogger(UaServerDatasheetParser.class);

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "https://example.org/ontology/sda#";
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";

    @Override
    public ParseResult parse(InputStream input) {
        if (input == null) {
            throw new ParserException("Input stream must not be null");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root;

            try {
                root = mapper.readTree(input);
            } catch (Exception e) {
                throw new ParserException("Invalid JSON: " + e.getMessage(), e);
            }

            if (root == null || root.isEmpty()) {
                throw new ParserException("JSON document is empty");
            }

            Model model = ModelFactory.createDefaultModel();
            model.setNsPrefix("sda", SDA_NS);
            model.setNsPrefix("inst", INST_NS);

            Property rdfType = model.createProperty(RDF_TYPE);
            Property rdfsLabel = model.createProperty(RDFS_LABEL);
            Resource bindingClass = model.createResource(SDA_NS + "CommunicationBinding");
            Resource connectorClass = model.createResource(SDA_NS + "DataConnector");
            Property protocol = model.createProperty(SDA_NS + "protocol");
            Property accessMode = model.createProperty(SDA_NS + "accessMode");
            Property securityMode = model.createProperty(SDA_NS + "securityMode");
            Property informationModel = model.createProperty(SDA_NS + "informationModel");
            Property semanticID = model.createProperty(SDA_NS + "semanticID");
            Property isGateway = model.createProperty(SDA_NS + "isGateway");
            Property hasBinding = model.createProperty(SDA_NS + "hasBinding");
            Property hasConnector = model.createProperty(SDA_NS + "hasConnector");
            Property image = model.createProperty(SDA_NS + "image");

            List<String> details = new ArrayList<>();
            int tripleCount = 0;

            // Parse EndpointDescriptions into sda:CommunicationBinding
            int endpointCount = 0;
            List<Resource> bindingResources = new ArrayList<>();
            JsonNode endpoints = root.path("EndpointDescriptions");
            if (endpoints.isArray()) {
                for (JsonNode endpoint : endpoints) {
                    String uri = endpoint.path("uri").asText("");
                    String secMode = endpoint.path("securityMode").asText("None");
                    String transportProfile = endpoint.path("transportProfile").asText("");

                    String bindingLocalName = "CB-UA-" + endpointCount;
                    Resource bindingRes = model.createResource(INST_NS + bindingLocalName);
                    bindingRes.addProperty(rdfType, bindingClass);
                    bindingRes.addProperty(rdfsLabel, "OPC-UA binding " + endpointCount);
                    bindingRes.addProperty(protocol, "OPC-UA");
                    bindingRes.addProperty(accessMode, "read-only");
                    bindingRes.addProperty(securityMode, secMode);

                    tripleCount += 5; // rdf:type + label + protocol + accessMode + securityMode
                    bindingResources.add(bindingRes);

                    details.add("CommunicationBinding: " + INST_NS + bindingLocalName
                        + " (uri=" + uri
                        + ", securityMode=" + secMode
                        + ", protocol=OPC-UA)");
                    endpointCount++;
                }
            }

            // Parse Configuration/NodeSets into sda:DataConnector
            int nodeSetCount = 0;
            List<Resource> connectorResources = new ArrayList<>();
            JsonNode configuration = root.path("Configuration");
            if (configuration.isObject()) {
                JsonNode nodeSets = configuration.path("NodeSets");
                if (nodeSets.isArray()) {
                    for (JsonNode nodeSet : nodeSets) {
                        String nodeSetId = nodeSet.path("id").asText("unknown");

                        String connectorLocalName = "DC-UA-" + nodeSetCount;
                        Resource connectorRes = model.createResource(INST_NS + connectorLocalName);
                        connectorRes.addProperty(rdfType, connectorClass);
                        connectorRes.addProperty(rdfsLabel, "OPC-UA connector " + nodeSetCount);
                        connectorRes.addProperty(informationModel, "OPC-UA");
                        connectorRes.addProperty(isGateway, model.createTypedLiteral(false));

                        tripleCount += 4; // rdf:type + label + informationModel + isGateway
                        connectorResources.add(connectorRes);

                        details.add("DataConnector: " + INST_NS + connectorLocalName
                            + " (nodeSet=" + nodeSetId + ")");
                        nodeSetCount++;
                    }
                }
            }

            // Parse Identification into device metadata
            Resource deviceRes = null;
            JsonNode identification = root.path("Identification");
            if (identification.isObject()) {
                String productUri = identification.path("productUri").asText("");
                String softwareVersion = identification.path("softwareVersion").asText("");
                String serialNumber = identification.path("serialNumber").asText("");

                if (!productUri.isBlank()) {
                    // Create a service resource representing the OPC UA server device
                    String deviceLocalName = "SVC-UA-Device";
                    deviceRes = model.createResource(INST_NS + deviceLocalName);
                    deviceRes.addProperty(rdfType, model.createResource(SDA_NS + "Service"));
                    deviceRes.addProperty(rdfsLabel, productUri);
                    if (!serialNumber.isBlank()) {
                        deviceRes.addProperty(image, "opcua/" + serialNumber);
                    }
                    tripleCount += 2; // rdf:type + label

                    details.add("Identification: productUri=" + productUri
                        + ", softwareVersion=" + softwareVersion);
                }
            }

            // Wire service → binding and service → connector via hasBinding/hasConnector
            if (deviceRes != null) {
                if (!bindingResources.isEmpty()) {
                    deviceRes.addProperty(hasBinding, bindingResources.get(0));
                    tripleCount++;
                }
                if (!connectorResources.isEmpty()) {
                    deviceRes.addProperty(hasConnector, connectorResources.get(0));
                    tripleCount++;
                }
            }

            String summary = "Parsed OPC UA Datasheet: " + tripleCount + " triples from "
                + endpointCount + " endpoints, "
                + nodeSetCount + " node sets";

            log.info("OPC UA Datasheet parse completed: {}", summary);

            return new ParseResult("OPC-UA-Datasheet", tripleCount, details, summary, model);

        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Failed to parse OPC UA Datasheet: " + e.getMessage(), e);
        }
    }
}
