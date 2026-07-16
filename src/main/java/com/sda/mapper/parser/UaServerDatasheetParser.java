package com.sda.mapper.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sda.mapper.error.ParserException;
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
 */
@Component
public class UaServerDatasheetParser implements Parser {

    private static final Logger log = LoggerFactory.getLogger(UaServerDatasheetParser.class);

    private static final String SDA_NS = "http://www.example.org/sda#";

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

            List<String> details = new ArrayList<>();
            int tripleCount = 0;

            // Parse EndpointDescriptions into sda:CommunicationBinding
            int endpointCount = 0;
            JsonNode endpoints = root.path("EndpointDescriptions");
            if (endpoints.isArray()) {
                for (JsonNode endpoint : endpoints) {
                    String uri = endpoint.path("uri").asText("");
                    String securityMode = endpoint.path("securityMode").asText("None");
                    String securityPolicy = endpoint.path("securityPolicy").asText("");
                    String transportProfile = endpoint.path("transportProfile").asText("");

                    String bindingUri = SDA_NS + "CommunicationBinding_" + endpointCount;
                    details.add("CommunicationBinding: " + bindingUri
                        + " (uri=" + uri
                        + ", securityMode=" + securityMode
                        + ", protocol=OPC-UA)");
                    tripleCount += 4; // rdf:type + uri + securityMode + transportProfile
                    endpointCount++;
                }
            }

            // Parse Configuration/NodeSets into sda:DataConnector
            int nodeSetCount = 0;
            JsonNode configuration = root.path("Configuration");
            if (configuration.isObject()) {
                JsonNode nodeSets = configuration.path("NodeSets");
                if (nodeSets.isArray()) {
                    for (JsonNode nodeSet : nodeSets) {
                        String nodeSetId = nodeSet.path("id").asText("unknown");

                        String connectorUri = SDA_NS + "DataConnector_" + nodeSetCount;
                        details.add("DataConnector: " + connectorUri
                            + " (nodeSet=" + nodeSetId + ")");
                        tripleCount += 2; // rdf:type + nodeSetId
                        nodeSetCount++;
                    }
                }
            }

            // Parse Identification into device metadata
            JsonNode identification = root.path("Identification");
            if (identification.isObject()) {
                String productUri = identification.path("productUri").asText("");
                String softwareVersion = identification.path("softwareVersion").asText("");

                if (!productUri.isBlank()) {
                    details.add("Identification: productUri=" + productUri
                        + ", softwareVersion=" + softwareVersion);
                    tripleCount += 2; // productUri + softwareVersion
                }
            }

            String summary = "Parsed OPC UA Datasheet: " + tripleCount + " triples from "
                + endpointCount + " endpoints, "
                + nodeSetCount + " node sets";

            log.info("OPC UA Datasheet parse completed: {}", summary);

            return new ParseResult("OPC-UA-Datasheet", tripleCount, details, summary);

        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Failed to parse OPC UA Datasheet: " + e.getMessage(), e);
        }
    }
}
