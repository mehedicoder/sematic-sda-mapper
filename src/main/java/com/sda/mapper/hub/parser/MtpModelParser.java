package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.ProcessVariable;
import com.sda.mapper.hub.model.Skill;
import com.sda.mapper.hub.model.SkillParameter;
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
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses MTP (Module Type Package) AML XML files, as used by
 * {@code device-descriptions/mtp}, into canonical entities: skills and skill
 * parameters from {@code Service} blocks, communication bindings from
 * {@code OpcUaServer} blocks and process variables from {@code OpcUaNode}
 * blocks of the data assembly.
 */
@Component
public class MtpModelParser implements CanonicalModelParser {

    private static final Logger log = LoggerFactory.getLogger(MtpModelParser.class);

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.MTP_AML;
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
                doc = factory.newDocumentBuilder().parse(
                    new ByteArrayInputStream(content));
            } catch (Exception e) {
                throw new ParsingException(fileName, "Invalid XML: " + e.getMessage(), e);
            }

            Element root = doc.getDocumentElement();
            if (root == null) {
                throw new ParsingException(fileName, "XML document has no root element");
            }
            String rootName = root.getLocalName() != null ? root.getLocalName() : root.getTagName();
            if (!"MtpAml".equalsIgnoreCase(rootName)) {
                throw new ParsingException(fileName, "Not an MTP AML document: unexpected root element '" + rootName + "'");
            }

            CanonicalAssetModel model = new CanonicalAssetModel();
            Map<String, String> parameterUnits = new LinkedHashMap<>();

            NodeList instances = doc.getElementsByTagName("Instance");
            for (int i = 0; i < instances.getLength(); i++) {
                Element instance = (Element) instances.item(i);
                String instanceName = attribute(instance, "Name", "Instance" + i);
                parseServices(instance, instanceName, fileName, model, parameterUnits);
                parseCommunication(instance, instanceName, fileName, model);
                parseDataAssembly(instance, instanceName, fileName, model, parameterUnits);
            }

            if (model.isEmpty()) {
                throw new ParsingException(fileName, "MTP document contains no services, servers or nodes");
            }
            log.info("MTP parse completed for '{}': {} entities", fileName, model.allEntities().size());
            return model;
        } catch (ParsingException e) {
            throw e;
        } catch (IOException e) {
            throw new ParsingException(fileName, "Failed to read MTP file: " + e.getMessage(), e);
        }
    }

    private void parseServices(Element instance, String instanceName, String fileName,
                               CanonicalAssetModel model, Map<String, String> parameterUnits) {
        NodeList services = instance.getElementsByTagName("Service");
        for (int i = 0; i < services.getLength(); i++) {
            Element service = (Element) services.item(i);
            String serviceName = attribute(service, "Name", "UnknownService" + i);
            String skillId = instanceName + "." + serviceName;
            String description = firstChildText(service, "Description");
            model.addEntity(new Skill(skillId, serviceName, description, null, fileName));

            NodeList parameters = service.getElementsByTagName("Parameter");
            for (int j = 0; j < parameters.getLength(); j++) {
                Element parameter = (Element) parameters.item(j);
                String paramName = attribute(parameter, "Name", "Parameter" + j);
                String unit = attribute(parameter, "EngineeringUnit", "");
                String min = attribute(parameter, "NominalRangeMin", "");
                String max = attribute(parameter, "NominalRangeMax", "");
                String dataType = attribute(parameter, "DataType", "");
                String functionClass = attribute(parameter, "FunctionClass", "");
                model.addEntity(new SkillParameter(skillId + "." + paramName, skillId, paramName,
                    functionClass, unit, min, max, dataType, fileName));
                parameterUnits.put(paramName, unit);
            }
        }
    }

    private void parseCommunication(Element instance, String instanceName, String fileName, CanonicalAssetModel model) {
        NodeList servers = instance.getElementsByTagName("OpcUaServer");
        for (int i = 0; i < servers.getLength(); i++) {
            Element server = (Element) servers.item(i);
            String endpoint = attribute(server, "Endpoint", "");
            if (endpoint.isBlank()) {
                continue;
            }
            String securityMode = attribute(server, "SecurityMode", "None");
            String securityPolicy = attribute(server, "SecurityPolicy", "");
            model.addEntity(new CommunicationBinding(endpoint, instanceName + " OPC UA binding",
                "OPC-UA", endpoint, securityMode, securityPolicy, fileName));
        }
    }

    private void parseDataAssembly(Element instance, String instanceName, String fileName,
                                   CanonicalAssetModel model, Map<String, String> parameterUnits) {
        NodeList nodes = instance.getElementsByTagName("OpcUaNode");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            String nodeId = attribute(node, "NodeId", "");
            if (nodeId.isBlank()) {
                continue;
            }
            String displayName = attribute(node, "DisplayName", "");
            String name = displayName.isBlank() ? attribute(node, "BrowseName", nodeId) : displayName;
            model.addEntity(new ProcessVariable(nodeId, name,
                parameterUnits.getOrDefault(name, ""), nodeId, instanceName, fileName));
        }
    }

    private static String attribute(Element element, String name, String fallback) {
        String value = element.getAttribute(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String firstChildText(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        if (children.getLength() > 0) {
            return children.item(0).getTextContent().trim();
        }
        return "";
    }
}
