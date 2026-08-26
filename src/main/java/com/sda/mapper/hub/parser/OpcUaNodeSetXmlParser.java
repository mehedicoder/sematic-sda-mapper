package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.DataConnector;
import com.sda.mapper.hub.model.SemanticConcept;
import com.sda.mapper.hub.model.Service;
import com.sda.mapper.hub.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses OPC UA NodeSet XML files (UANodeSet format) into canonical entities,
 * mirroring the semantics of the pipeline {@code OpcUaNodeSetParser} but emitting
 * {@link CanonicalAssetModel} entities so the same file works through the
 * Mapping Hub UI.
 *
 * <p>Extracts {@link DataConnector}s from {@code <UAVariable>}/{@code <UAObject>}
 * nodes, {@link Skill}s from {@code <UAMethod>} nodes, a generic
 * {@link CommunicationBinding} (OPC-UA) and a {@link Service} that wires them
 * together.</p>
 */
@Component
public class OpcUaNodeSetXmlParser implements CanonicalModelParser {

    private static final Logger log = LoggerFactory.getLogger(OpcUaNodeSetXmlParser.class);

    private static final String UA_NS = "http://opcfoundation.org/UA/2011/03/UANodeSet.xsd";

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.OPCUA_XML;
    }

    @Override
    public CanonicalAssetModel parse(InputStream input, String fileName) {
        if (input == null) {
            throw new ParsingException(fileName, "Input stream must not be null");
        }
        try {
            byte[] content = input.readAllBytes();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc;
            try {
                doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(content));
            } catch (Exception e) {
                throw new ParsingException(fileName, "Invalid XML: " + e.getMessage(), e);
            }

            Element root = doc.getDocumentElement();
            if (root == null) {
                throw new ParsingException(fileName, "XML document has no root element");
            }
            if (!isNodeSet(root)) {
                throw new ParsingException(fileName, "Not an OPC UA NodeSet XML file");
            }

            CanonicalAssetModel model = new CanonicalAssetModel();
            Map<String, String> aliases = parseAliases(doc);

            NodeList uaObjects = root.getElementsByTagNameNS(UA_NS, "UAObject");
            NodeList uaVariables = root.getElementsByTagNameNS(UA_NS, "UAVariable");
            NodeList uaMethods = root.getElementsByTagNameNS(UA_NS, "UAMethod");

            List<String> connectorIds = new ArrayList<>();
            for (int i = 0; i < uaVariables.getLength(); i++) {
                Element el = (Element) uaVariables.item(i);
                String browseName = el.getAttribute("BrowseName");
                String displayName = getDisplayName(el);
                String nodeId = el.getAttribute("NodeId");
                String name = displayName != null && !displayName.isBlank() ? displayName : browseName;
                String id = "DC-UA-" + sanitize(browseName.isEmpty() ? nodeId : browseName);
                model.addEntity(new DataConnector(id, name, "OPC UA variable " + nodeId,
                    nodeId, fileName));
                connectorIds.add(id);
            }
            for (int i = 0; i < uaObjects.getLength(); i++) {
                Element el = (Element) uaObjects.item(i);
                String browseName = el.getAttribute("BrowseName");
                String displayName = getDisplayName(el);
                String nodeId = el.getAttribute("NodeId");
                String name = displayName != null && !displayName.isBlank() ? displayName : browseName;
                String id = "DC-UA-" + sanitize(browseName.isEmpty() ? nodeId : browseName);
                model.addEntity(new DataConnector(id, name, "OPC UA object " + nodeId,
                    nodeId, fileName));
                connectorIds.add(id);
            }

            List<String> skillIds = new ArrayList<>();
            for (int i = 0; i < uaMethods.getLength(); i++) {
                Element el = (Element) uaMethods.item(i);
                String browseName = el.getAttribute("BrowseName");
                String displayName = getDisplayName(el);
                String nodeId = el.getAttribute("NodeId");
                String name = displayName != null && !displayName.isBlank() ? displayName : browseName;
                String id = "SK-UA-" + sanitize(browseName.isEmpty() ? nodeId : browseName);
                model.addEntity(new Skill(id, name, "OPC UA method " + nodeId, null, fileName));
                skillIds.add(id);
            }

            model.addEntity(new CommunicationBinding("opc.tcp://unspecified",
                "OPC UA NodeSet binding", "OPC-UA", "opc.tcp://unspecified",
                "None", "", fileName));

            if (!connectorIds.isEmpty()) {
                model.addEntity(new Service("SVC-UA-NodeSet-0", "OPC UA NodeSet service",
                    "Service derived from OPC UA NodeSet", "OPC-UA", fileName));
            }

            for (String ns : parseNamespaceUris(doc)) {
                model.addEntity(new SemanticConcept("SC-" + sanitize(ns), ns,
                    "Namespace " + ns, ns, fileName));
            }

            if (model.isEmpty()) {
                throw new ParsingException(fileName, "OPC UA NodeSet contains no variables, objects or methods");
            }
            log.info("OPC UA NodeSet parse completed for '{}': {} entities", fileName, model.allEntities().size());
            return model;
        } catch (ParsingException e) {
            throw e;
        } catch (IOException e) {
            throw new ParsingException(fileName, "Failed to read NodeSet file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParsingException(fileName, "Failed to parse OPC UA NodeSet: " + e.getMessage(), e);
        }
    }

    private boolean isNodeSet(Element root) {
        String localName = root.getLocalName();
        if ("UANodeSet".equals(localName)) {
            return true;
        }
        String ns = root.getNamespaceURI();
        return ns != null && ns.contains("opcfoundation.org") && "UANodeSet".equals(localName);
    }

    private Map<String, String> parseAliases(Document doc) {
        Map<String, String> aliases = new java.util.HashMap<>();
        NodeList aliasNodes = doc.getElementsByTagNameNS(UA_NS, "Alias");
        for (int i = 0; i < aliasNodes.getLength(); i++) {
            Element alias = (Element) aliasNodes.item(i);
            String aliasName = alias.getTextContent().trim();
            String nodeId = alias.getAttribute("NodeId");
            if (aliasName != null && !aliasName.isBlank() && nodeId != null) {
                aliases.put(aliasName, nodeId);
            }
        }
        return aliases;
    }

    private Set<String> parseNamespaceUris(Document doc) {
        Set<String> namespaceUris = new LinkedHashSet<>();
        NodeList nsNodes = doc.getElementsByTagNameNS(UA_NS, "NamespaceUris");
        if (nsNodes.getLength() > 0) {
            Element nsContainer = (Element) nsNodes.item(0);
            NodeList uriNodes = nsContainer.getElementsByTagNameNS(UA_NS, "Uri");
            for (int i = 0; i < uriNodes.getLength(); i++) {
                namespaceUris.add(uriNodes.item(i).getTextContent().trim());
            }
        }
        return namespaceUris;
    }

    private String getDisplayName(Element el) {
        NodeList nameNodes = el.getElementsByTagNameNS(UA_NS, "DisplayName");
        if (nameNodes.getLength() > 0) {
            return nameNodes.item(0).getTextContent().trim();
        }
        return null;
    }

    private String sanitize(String name) {
        if (name == null) {
            return "unknown";
        }
        return name.replaceAll("[^a-zA-Z0-9._\\-]", "_").replaceAll("_+", "_");
    }
}
