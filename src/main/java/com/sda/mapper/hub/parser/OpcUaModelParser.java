package com.sda.mapper.hub.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sda.mapper.hub.model.Asset;
import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.ProcessVariable;
import com.sda.mapper.hub.model.SemanticConcept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Parses OPC UA server datasheets in the JSON format used by
 * {@code device-descriptions/opcua} into canonical entities: assets from the
 * identification block, communication bindings from endpoint descriptions and
 * process variables plus semantic concepts from the configured node sets.
 */
@Component
public class OpcUaModelParser implements CanonicalModelParser {

    private static final Logger log = LoggerFactory.getLogger(OpcUaModelParser.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.OPCUA_JSON;
    }

    @Override
    public CanonicalAssetModel parse(InputStream input, String fileName) {
        if (input == null) {
            throw new ParsingException(fileName, "Input stream must not be null");
        }
        try {
            JsonNode root = mapper.readTree(input);
            if (root == null || root.isNull()) {
                throw new ParsingException(fileName, "JSON document is empty");
            }

            CanonicalAssetModel model = new CanonicalAssetModel();
            parseIdentification(root, fileName, model);
            parseEndpoints(root, fileName, model);
            parseNodeSets(root, fileName, model);

            if (model.isEmpty()) {
                throw new ParsingException(fileName,
                    "Not an OPC UA datasheet: missing identification, endpoints or node sets");
            }
            log.info("OPC UA parse completed for '{}': {} entities", fileName, model.allEntities().size());
            return model;
        } catch (ParsingException e) {
            throw e;
        } catch (IOException e) {
            throw new ParsingException(fileName, "Invalid JSON: " + e.getMessage(), e);
        }
    }

    private void parseIdentification(JsonNode root, String fileName, CanonicalAssetModel model) {
        JsonNode identification = root.path("Identification");
        if (!identification.isObject() || identification.isEmpty()) {
            return;
        }
        String productUri = text(identification, "productUri");
        String manufacturer = text(identification, "manufacturerName");
        String serialNumber = text(identification, "serialNumber");
        String name = manufacturer.isBlank() ? serialNumber : manufacturer;
        String assetId = productUri.isBlank() ? serialNumber : productUri;
        if (!assetId.isBlank()) {
            model.addEntity(new Asset(assetId, name, manufacturer, serialNumber, "Instance", fileName));
        }
    }

    private void parseEndpoints(JsonNode root, String fileName, CanonicalAssetModel model) {
        JsonNode endpoints = root.path("EndpointDescriptions");
        if (!endpoints.isArray()) {
            return;
        }
        for (JsonNode endpoint : endpoints) {
            String uri = text(endpoint, "uri");
            if (uri.isBlank()) {
                continue;
            }
            String securityMode = text(endpoint, "securityMode");
            String securityPolicy = text(endpoint, "securityPolicy");
            model.addEntity(new CommunicationBinding(uri, "OPC UA binding", "OPC-UA",
                uri, securityMode, securityPolicy, fileName));
        }
    }

    private void parseNodeSets(JsonNode root, String fileName, CanonicalAssetModel model) {
        JsonNode configuration = root.path("Configuration");
        JsonNode nodeSets = configuration.path("NodeSets");
        if (!nodeSets.isArray()) {
            return;
        }
        Set<String> namespaces = new LinkedHashSet<>();
        for (JsonNode nodeSet : nodeSets) {
            String nodeId = text(nodeSet, "id");
            String namespace = text(nodeSet, "namespace");
            if (nodeId.isBlank()) {
                continue;
            }
            model.addEntity(new ProcessVariable(nodeId, variableName(nodeId), "",
                nodeId, namespace, fileName));
            if (!namespace.isBlank()) {
                namespaces.add(namespace);
            }
        }
        for (String namespace : namespaces) {
            model.addEntity(new SemanticConcept("SC-" + namespace, namespace,
                "Namespace " + namespace, namespace, fileName));
        }
    }

    private static String variableName(String nodeId) {
        int lastDot = nodeId.lastIndexOf('.');
        String tail = lastDot >= 0 ? nodeId.substring(lastDot + 1) : nodeId;
        int lastSep = tail.lastIndexOf('=');
        return (lastSep >= 0 ? tail.substring(lastSep + 1) : tail).trim();
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.path(field).asText("").trim();
    }
}
