package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses OPC UA NodeSet XML files (UANodeSet format) into SDA semantic triples.
 * Extracts DataConnectors from {@code <UAVariable>}/{@code <UAObject>} nodes,
 * Skills from {@code <UAMethod>} nodes, and CommunicationBindings from
 * the implicit OPC UA server endpoint.
 */
@Component
public class OpcUaNodeSetParser implements Parser {

    private static final Logger log = LoggerFactory.getLogger(OpcUaNodeSetParser.class);

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String CSS_NS = "http://www.example.org/css#";
    private static final String UA_NS = "http://opcfoundation.org/UA/2011/03/UANodeSet.xsd";
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";

    @Override
    public ParseResult parse(InputStream input) {
        if (input == null) {
            throw new ParserException("Input stream must not be null");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc;

            try {
                doc = builder.parse(input);
            } catch (Exception e) {
                throw new ParserException("Invalid XML: " + e.getMessage(), e);
            }

            Element root = doc.getDocumentElement();
            if (root == null) {
                throw new ParserException("XML document has no root element");
            }

            if (!isNodeSet(root)) {
                throw new ParserException("Not an OPC UA NodeSet XML file");
            }

            Model model = ModelFactory.createDefaultModel();
            model.setNsPrefix("sda", SDA_NS);
            model.setNsPrefix("css", CSS_NS);
            model.setNsPrefix("inst", INST_NS);

            Property rdfType = model.createProperty(RDF_TYPE);
            Property rdfsLabel = model.createProperty(RDFS_LABEL);
            Resource connectorClass = model.createResource(SDA_NS + "DataConnector");
            Resource skillClass = model.createResource(CSS_NS + "Skill");
            Resource bindingClass = model.createResource(SDA_NS + "CommunicationBinding");
            Resource serviceClass = model.createResource(SDA_NS + "Service");
            Property informationModel = model.createProperty(SDA_NS + "informationModel");
            Property protocol = model.createProperty(SDA_NS + "protocol");
            Property accessMode = model.createProperty(SDA_NS + "accessMode");
            Property isImplementedBy = model.createProperty(SDA_NS + "isImplementedBy");
            Property usesBinding = model.createProperty(SDA_NS + "usesBinding");
            Property usesConnector = model.createProperty(SDA_NS + "usesConnector");
            Property uaNodeId = model.createProperty(SDA_NS + "uaNodeId");
            Property uaBrowseName = model.createProperty(SDA_NS + "uaBrowseName");
            Property dataType = model.createProperty(SDA_NS + "dataType");

            List<String> details = new ArrayList<>();
            int tripleCount = 0;

            Map<String, String> aliases = parseAliases(doc);
            List<String> namespaceUris = parseNamespaceUris(doc);

            NodeList uaObjects = root.getElementsByTagNameNS(UA_NS, "UAObject");
            NodeList uaVariables = root.getElementsByTagNameNS(UA_NS, "UAVariable");
            NodeList uaMethods = root.getElementsByTagNameNS(UA_NS, "UAMethod");
            NodeList uaObjectTypes = root.getElementsByTagNameNS(UA_NS, "UAObjectType");
            NodeList uaVariableTypes = root.getElementsByTagNameNS(UA_NS, "UAVariableType");
            NodeList uaDataTypes = root.getElementsByTagNameNS(UA_NS, "UADataType");

            List<Resource> connectorResources = new ArrayList<>();

            for (int i = 0; i < uaVariables.getLength(); i++) {
                Element el = (Element) uaVariables.item(i);
                String nodeId = el.getAttribute("NodeId");
                String browseName = el.getAttribute("BrowseName");
                String displayName = getDisplayName(el);
                String dt = el.getAttribute("DataType");

                String localName = sanitizeLocalName("DC-UA-" + browseName);
                Resource res = model.createResource(INST_NS + localName);
                res.addProperty(rdfType, connectorClass);
                res.addProperty(rdfsLabel, displayName != null ? displayName : browseName);
                res.addProperty(informationModel, "OPC-UA");
                res.addProperty(uaNodeId, nodeId);
                if (browseName != null && !browseName.isBlank()) {
                    res.addProperty(uaBrowseName, browseName);
                }
                if (dt != null && !dt.isBlank()) {
                    String resolvedDt = aliases.getOrDefault(dt, dt);
                    res.addProperty(dataType, resolvedDt);
                }

                tripleCount += 4;
                connectorResources.add(res);
                details.add("DataConnector: " + INST_NS + localName
                    + " (nodeId=" + nodeId + ", browseName=" + browseName + ")");
            }

            for (int i = 0; i < uaObjects.getLength(); i++) {
                Element el = (Element) uaObjects.item(i);
                String nodeId = el.getAttribute("NodeId");
                String browseName = el.getAttribute("BrowseName");
                String displayName = getDisplayName(el);
                String typeDef = resolveTypeDef(el, aliases);

                String localName = sanitizeLocalName("DC-UA-" + browseName);
                Resource res = model.createResource(INST_NS + localName);
                res.addProperty(rdfType, connectorClass);
                res.addProperty(rdfsLabel, displayName != null ? displayName : browseName);
                res.addProperty(informationModel, "OPC-UA");
                res.addProperty(uaNodeId, nodeId);
                if (browseName != null && !browseName.isBlank()) {
                    res.addProperty(uaBrowseName, browseName);
                }
                if (typeDef != null && !typeDef.isBlank()) {
                    res.addProperty(dataType, typeDef);
                }

                tripleCount += 4;
                connectorResources.add(res);
                details.add("DataConnector: " + INST_NS + localName
                    + " (nodeId=" + nodeId + ", browseName=" + browseName + ")");
            }

            List<Resource> skillResources = new ArrayList<>();
            for (int i = 0; i < uaMethods.getLength(); i++) {
                Element el = (Element) uaMethods.item(i);
                String nodeId = el.getAttribute("NodeId");
                String browseName = el.getAttribute("BrowseName");
                String displayName = getDisplayName(el);

                String localName = sanitizeLocalName("SK-UA-" + browseName);
                Resource res = model.createResource(INST_NS + localName);
                res.addProperty(rdfType, skillClass);
                res.addProperty(rdfsLabel, displayName != null ? displayName : browseName);
                res.addProperty(uaNodeId, nodeId);
                if (browseName != null && !browseName.isBlank()) {
                    res.addProperty(uaBrowseName, browseName);
                }

                tripleCount += 3;
                skillResources.add(res);
                details.add("Skill: " + INST_NS + localName
                    + " (nodeId=" + nodeId + ", browseName=" + browseName + ")");
            }

            String bindingLocalName = "CB-UA-NodeSet-0";
            Resource bindingRes = model.createResource(INST_NS + bindingLocalName);
            bindingRes.addProperty(rdfType, bindingClass);
            bindingRes.addProperty(rdfsLabel, "OPC UA NodeSet binding");
            bindingRes.addProperty(protocol, "OPC-UA");
            bindingRes.addProperty(accessMode, "read-only");
            tripleCount += 4;
            details.add("CommunicationBinding: " + INST_NS + bindingLocalName + " (protocol=OPC-UA)");

            if (!connectorResources.isEmpty()) {
                String serviceLocalName = "SVC-UA-NodeSet-0";
                Resource serviceRes = model.createResource(INST_NS + serviceLocalName);
                serviceRes.addProperty(rdfType, serviceClass);
                serviceRes.addProperty(rdfsLabel, "OPC UA NodeSet service");
                serviceRes.addProperty(usesBinding, bindingRes);
                tripleCount += 3;
                details.add("Service: " + INST_NS + serviceLocalName);

                bindingRes.addProperty(usesConnector, connectorResources.get(0));
                tripleCount++;

                for (Resource skillRes : skillResources) {
                    skillRes.addProperty(isImplementedBy, serviceRes);
                    tripleCount++;
                }
            }

            int typeCount = uaObjectTypes.getLength() + uaVariableTypes.getLength() + uaDataTypes.getLength();
            String summary = "Parsed OPC UA NodeSet: " + tripleCount + " triples from "
                + uaVariables.getLength() + " variables, "
                + uaObjects.getLength() + " objects, "
                + uaMethods.getLength() + " methods, "
                + typeCount + " type definitions";

            log.info("OPC UA NodeSet parse completed: {}", summary);

            return new ParseResult("OPC-UA-NodeSet", tripleCount, details, summary, model);

        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Failed to parse OPC UA NodeSet: " + e.getMessage(), e);
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
        Map<String, String> aliases = new HashMap<>();
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

    private List<String> parseNamespaceUris(Document doc) {
        List<String> namespaceUris = new ArrayList<>();
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

    private String resolveTypeDef(Element el, Map<String, String> aliases) {
        NodeList refNodes = el.getElementsByTagNameNS(UA_NS, "Reference");
        for (int i = 0; i < refNodes.getLength(); i++) {
            Element ref = (Element) refNodes.item(i);
            String refType = ref.getAttribute("ReferenceType");
            if ("HasTypeDefinition".equals(refType)) {
                String target = ref.getTextContent().trim();
                return aliases.getOrDefault(target, target);
            }
        }
        return null;
    }

    private String sanitizeLocalName(String name) {
        if (name == null) {
            return "unknown";
        }
        return name.replaceAll("[^a-zA-Z0-9._\\-]", "_").replaceAll("_+", "_");
    }
}
