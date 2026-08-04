package com.sda.mapper.hub.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sda.mapper.hub.model.Asset;
import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.DataConnector;
import com.sda.mapper.hub.model.Observation;
import com.sda.mapper.hub.model.ProcessVariable;
import com.sda.mapper.hub.model.SafetyClass;
import com.sda.mapper.hub.model.SemanticConcept;
import com.sda.mapper.hub.model.Service;
import com.sda.mapper.hub.model.Skill;
import com.sda.mapper.hub.model.SkillParameter;
import com.sda.mapper.hub.model.UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses AAS (Asset Administration Shell) files, both the JSON AAS sample
 * format used by {@code device-descriptions/aas} and a best-effort XML variant,
 * into canonical entities: assets, skills, skill parameters, communication
 * bindings, use cases (operations), process variables, observations, data
 * connectors, semantic concepts and safety classes.
 */
@Component
public class AasModelParser implements CanonicalModelParser {

    private static final Logger log = LoggerFactory.getLogger(AasModelParser.class);
    private static final String SKILLS = "Skills";
    private static final String OPERATIONS = "Operations";
    private static final String PROCESS_VALUES = "ProcessValues";
    private static final String COMMUNICATION = "Communication";
    private static final String TECHNICAL_DATA = "TechnicalData";

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.AAS_JSON;
    }

    @Override
    public boolean supports(AssetFileFormat format) {
        return format == AssetFileFormat.AAS_JSON || format == AssetFileFormat.AAS_XML;
    }

    @Override
    public CanonicalAssetModel parse(InputStream input, String fileName) {
        if (input == null) {
            throw new ParsingException(fileName, "Input stream must not be null");
        }
        try {
            byte[] content = input.readAllBytes();
            String text = new String(content, java.nio.charset.StandardCharsets.UTF_8);
            if (text.isBlank()) {
                throw new ParsingException(fileName, "AAS document is empty");
            }
            CanonicalAssetModel model = text.trim().startsWith("<")
                ? parseXml(text, fileName)
                : parseJson(text, fileName);
            log.info("AAS parse completed for '{}': {} entities", fileName, model.allEntities().size());
            return model;
        } catch (ParsingException e) {
            throw e;
        } catch (IOException e) {
            throw new ParsingException(fileName, "Failed to read AAS file: " + e.getMessage(), e);
        }
    }

    private CanonicalAssetModel parseJson(String text, String fileName) {
        JsonNode root;
        try {
            root = mapper.readTree(text);
        } catch (IOException e) {
            throw new ParsingException(fileName, "Invalid JSON: " + e.getMessage(), e);
        }
        JsonNode shells = root.path("assetAdministrationShells");
        if (!shells.isArray()) {
            throw new ParsingException(fileName, "Missing 'assetAdministrationShells' array");
        }

        CanonicalAssetModel model = new CanonicalAssetModel();
        Map<String, String> skillUnits = new LinkedHashMap<>();

        for (JsonNode shell : shells) {
            String shellIdShort = text(shell, "idShort");
            String globalAssetId = text(shell.path("assetInformation"), "globalAssetId");
            String shellId = text(shell.path("id"), "identifier");

            String manufacturer = "";
            String serialNumber = "";
            List<JsonNode> submodels = new ArrayList<>();
            JsonNode shellSubmodels = shell.path("submodels");
            if (shellSubmodels.isArray()) {
                shellSubmodels.forEach(submodels::add);
            }
            JsonNode rootSubmodels = root.path("submodels");
            if (rootSubmodels.isArray()) {
                rootSubmodels.forEach(submodels::add);
            }

            for (JsonNode submodel : submodels) {
                String subIdShort = text(submodel, "idShort");
                switch (subIdShort) {
                    case "Identification" -> {
                        manufacturer = propertyValue(submodel, "Manufacturer");
                        serialNumber = propertyValue(submodel, "SerialNumber");
                    }
                    case COMMUNICATION -> {
                        CommunicationBinding binding = parseCommunicationBinding(submodel, fileName);
                        if (binding != null) {
                            model.addEntity(binding);
                            model.addEntity(new Service("SVC-" + shellIdShort, shellIdShort + " Service",
                                "Exposed by AAS " + shellIdShort, "OPC-UA", fileName));
                        }
                    }
                    case SKILLS -> parseSkills(submodel, shellIdShort, fileName, model, skillUnits);
                    case OPERATIONS -> parseOperations(submodel, shellIdShort, fileName, model);
                    case TECHNICAL_DATA -> parseTechnicalData(submodel, shellIdShort, fileName, model);
                    default -> { }
                }
            }

            if (!globalAssetId.isBlank() || !shellIdShort.isBlank()) {
                String assetId = globalAssetId.isBlank() ? shellIdShort : globalAssetId;
                String kind = text(shell.path("assetInformation"), "assetKind");
                model.addEntity(new Asset(assetId, shellIdShort, manufacturer, serialNumber,
                    kind.isBlank() ? "Instance" : kind, fileName));
            }

            JsonNode processValues = findSubmodel(submodels, PROCESS_VALUES);
            if (processValues != null) {
                parseProcessValues(processValues, shellIdShort, fileName, model, skillUnits);
            }
        }

        if (model.isEmpty() && shells.isEmpty()) {
            throw new ParsingException(fileName, "AAS document contains no administration shells");
        }
        return model;
    }

    private CanonicalAssetModel parseXml(String text, String fileName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(
                new java.io.ByteArrayInputStream(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            if (root == null || !root.getLocalName().toLowerCase().contains("assetadministrationshell")) {
                throw new ParsingException(fileName, "Not an AAS XML document: unexpected root element");
            }

            CanonicalAssetModel model = new CanonicalAssetModel();
            NodeList shells = root.getElementsByTagNameNS("*", "assetAdministrationShell");
            if (shells.getLength() == 0 && "assetAdministrationShell".equalsIgnoreCase(root.getLocalName())) {
                shells = new SingleNodeList(root);
            }

            for (int i = 0; i < shells.getLength(); i++) {
                Element shell = (Element) shells.item(i);
                String idShort = childText(shell, "idShort");
                String globalAssetId = childText(shell, "globalAssetId");
                String manufacturer = childProperty(shell, "Manufacturer");
                String serialNumber = childProperty(shell, "SerialNumber");
                String assetId = globalAssetId.isBlank() ? idShort : globalAssetId;
                if (!assetId.isBlank()) {
                    model.addEntity(new Asset(assetId, idShort, manufacturer, serialNumber, "Instance", fileName));
                }
                NodeList bindings = shell.getElementsByTagNameNS("*", "CommunicationBinding");
                for (int b = 0; b < bindings.getLength(); b++) {
                    Element binding = (Element) bindings.item(b);
                    String endpoint = childText(binding, "endpointUrl");
                    if (!endpoint.isBlank()) {
                        model.addEntity(new CommunicationBinding(endpoint, "OPC UA binding", "OPC-UA",
                            endpoint, childText(binding, "securityMode"), childText(binding, "securityPolicy"), fileName));
                    }
                }
            }
            if (model.isEmpty()) {
                throw new ParsingException(fileName, "AAS XML document contains no asset administration shells");
            }
            return model;
        } catch (ParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingException(fileName, "Invalid AAS XML: " + e.getMessage(), e);
        }
    }

    private void parseSkills(JsonNode submodel, String shellIdShort, String fileName,
                             CanonicalAssetModel model, Map<String, String> skillUnits) {
        JsonNode elements = submodel.path("submodelElements");
        if (!elements.isArray()) {
            return;
        }
        for (JsonNode element : elements) {
            String idShort = text(element, "idShort");
            if (idShort.isBlank()) {
                continue;
            }
            String skillId = propertyValue(element, "SkillId");
            if (skillId.isBlank()) {
                skillId = firstSemanticId(element);
            }
            if (skillId.isBlank()) {
                skillId = idShort;
            }
            String name = propertyValue(element, "SkillName");
            if (name.isBlank()) {
                name = idShort;
            }
            String description = propertyValue(element, "Description");
            String unit = propertyValue(element, "EngineeringUnit");
            String min = propertyValue(element, "NominalRangeMin");
            String max = propertyValue(element, "NominalRangeMax");
            String functionClass = propertyValue(element, "FunctionClass");

            model.addEntity(new Skill(skillId, name, description, null, fileName));
            if (!unit.isBlank()) {
                skillUnits.put(name, unit);
                model.addEntity(new SkillParameter(skillId + ".Range", skillId, "NominalRange",
                    functionClass, unit, min, max, "", fileName));
            }
            addSemanticConcept(firstSemanticId(element), fileName, model);
        }
        log.debug("AAS '{}' skills parsed from '{}'", shellIdShort, fileName);
    }

    private void parseOperations(JsonNode submodel, String shellIdShort, String fileName, CanonicalAssetModel model) {
        JsonNode elements = submodel.path("submodelElements");
        if (!elements.isArray()) {
            return;
        }
        for (JsonNode element : elements) {
            String idShort = text(element, "idShort");
            if (idShort.isBlank()) {
                continue;
            }
            String iri = firstSemanticId(element);
            model.addEntity(new UseCase(iri.isBlank() ? idShort : iri, idShort, "", "", fileName));
        }
    }

    private void parseTechnicalData(JsonNode submodel, String shellIdShort, String fileName, CanonicalAssetModel model) {
        String ne107 = propertyValue(submodel, "NE107Support");
        String ip = propertyValue(submodel, "IngressProtection");
        if ("true".equalsIgnoreCase(ne107) || !ip.isBlank()) {
            String level = "true".equalsIgnoreCase(ne107) ? "NE107" : "IP" + ip;
            model.addEntity(new SafetyClass("SCL-" + shellIdShort, shellIdShort + " Safety Class",
                "Derived from AAS TechnicalData", level, fileName));
        }
    }

    private void parseProcessValues(JsonNode submodel, String shellIdShort, String fileName,
                                    CanonicalAssetModel model, Map<String, String> skillUnits) {
        JsonNode elements = submodel.path("submodelElements");
        if (!elements.isArray()) {
            return;
        }
        String submodelId = text(submodel.path("id"), "identifier");
        model.addEntity(new DataConnector("DC-" + shellIdShort + "-processvalues",
            "ProcessValues connector", "Values exposed by AAS " + shellIdShort,
            submodelId.isBlank() ? shellIdShort + "/ProcessValues" : submodelId, fileName));

        for (JsonNode element : elements) {
            String idShort = text(element, "idShort");
            if (idShort.isBlank()) {
                continue;
            }
            String semanticId = firstSemanticId(element);
            String unit = skillUnits.getOrDefault(idShort, "");
            model.addEntity(new ProcessVariable(idShort, idShort, unit,
                semanticId.isBlank() ? shellIdShort + "." + idShort : semanticId, shellIdShort, fileName));
            String value = text(element, "value");
            if (!value.isBlank()) {
                model.addEntity(new Observation(idShort + "-observation", idShort, "",
                    value, "", fileName));
            }
            addSemanticConcept(semanticId, fileName, model);
        }
    }

    private CommunicationBinding parseCommunicationBinding(JsonNode submodel, String fileName) {
        String endpoint = propertyValue(submodel, "EndpointUrl");
        if (endpoint.isBlank()) {
            endpoint = propertyValue(submodel, "NodeId");
        }
        if (endpoint.isBlank()) {
            return null;
        }
        String protocol = propertyValue(submodel, "Protocol");
        if (protocol.isBlank()) {
            protocol = "OPC-UA";
        }
        String securityMode = propertyValue(submodel, "SecurityMode");
        String securityPolicy = propertyValue(submodel, "SecurityPolicy");
        return new CommunicationBinding(endpoint, "OPC UA binding", protocol,
            endpoint, securityMode, securityPolicy, fileName);
    }

    private void addSemanticConcept(String iri, String fileName, CanonicalAssetModel model) {
        if (iri.isBlank()) {
            return;
        }
        String name = iri.substring(Math.max(0, iri.lastIndexOf('/') + 1));
        if (name.isBlank()) {
            name = iri;
        }
        model.addEntity(new SemanticConcept("SC-" + name, name, "Semantic reference " + name, iri, fileName));
    }

    private JsonNode findSubmodel(List<JsonNode> submodels, String idShort) {
        for (JsonNode submodel : submodels) {
            if (idShort.equals(text(submodel, "idShort"))) {
                return submodel;
            }
        }
        return null;
    }

    private static String propertyValue(JsonNode node, String name) {
        JsonNode properties = node.path("properties");
        if (!properties.isArray()) {
            return "";
        }
        for (JsonNode prop : properties) {
            if (name.equals(text(prop, "idShort"))) {
                return text(prop, "value");
            }
        }
        return "";
    }

    private static String firstSemanticId(JsonNode node) {
        JsonNode semanticId = node.path("semanticId");
        JsonNode keys = semanticId.path("keys");
        if (keys.isArray() && !keys.isEmpty()) {
            return text(keys.get(0), "value");
        }
        return "";
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.path(field).asText("").trim();
    }

    private static String childText(Element parent, String localName) {
        NodeList children = parent.getElementsByTagNameNS("*", localName);
        if (children.getLength() > 0) {
            return children.item(0).getTextContent().trim();
        }
        return "";
    }

    private static String childProperty(Element parent, String propertyName) {
        NodeList properties = parent.getElementsByTagNameNS("*", "property");
        for (int i = 0; i < properties.getLength(); i++) {
            Element prop = (Element) properties.item(i);
            if (propertyName.equals(prop.getAttribute("name"))) {
                return prop.getTextContent().trim();
            }
        }
        return "";
    }

    /**
     * Minimal NodeList wrapper for a single element (used for the root shell).
     */
    private record SingleNodeList(Element element) implements NodeList {

        @Override
        public int getLength() {
            return 1;
        }

        @Override
        public Node item(int index) {
            return index == 0 ? element : null;
        }
    }
}
