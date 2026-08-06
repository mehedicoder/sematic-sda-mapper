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
 * Parses AAS (Asset Administration Shell) JSON files into SDA semantic triples.
 * Extracts CommunicationBindings from the Communication submodel,
 * Skills from the Skills submodel, DataConnectors from ProcessValues,
 * and Service metadata from the Identification submodel.
 * Produces a Jena Model containing instantiated ABox triples.
 */
@Component
public class AasParser implements Parser {

    private static final Logger log = LoggerFactory.getLogger(AasParser.class);

    private static final String INST_NS = "http://sda-platform.org/instance#";
    private static final String SDA_NS = "http://sda-platform.org/ontology#";
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

            JsonNode shells = root.path("assetAdministrationShells");
            if (!shells.isArray() || shells.isEmpty()) {
                throw new ParserException(
                    "Not an AAS JSON file: missing or empty 'assetAdministrationShells' array");
            }

            Model model = ModelFactory.createDefaultModel();
            model.setNsPrefix("sda", SDA_NS);
            model.setNsPrefix("inst", INST_NS);

            Property rdfType = model.createProperty(RDF_TYPE);
            Property rdfsLabel = model.createProperty(RDFS_LABEL);
            Resource skillClass = model.createResource(SDA_NS + "Skill");
            Resource bindingClass = model.createResource(SDA_NS + "CommunicationBinding");
            Resource connectorClass = model.createResource(SDA_NS + "DataConnector");
            Property protocol = model.createProperty(SDA_NS + "protocol");
            Property accessMode = model.createProperty(SDA_NS + "accessMode");
            Property securityMode = model.createProperty(SDA_NS + "securityMode");
            Property isImplementedBy = model.createProperty(SDA_NS + "isImplementedBy");
            Property hasBinding = model.createProperty(SDA_NS + "hasBinding");
            Property hasConnector = model.createProperty(SDA_NS + "hasConnector");

            List<String> details = new ArrayList<>();
            int tripleCount = 0;

            for (JsonNode shell : shells) {
                String idShort = shell.path("idShort").asText("UnknownAAS");

                // Parse submodels within this AAS
                JsonNode submodels = shell.path("submodels");
                if (submodels.isArray()) {
                    for (JsonNode submodel : submodels) {
                        String submodelIdShort = submodel.path("idShort").asText("");

                        if ("Communication".equals(submodelIdShort)) {
                            tripleCount += parseCommunicationSubmodel(
                                submodel, model, rdfType, rdfsLabel, bindingClass,
                                protocol, accessMode, securityMode, details);
                        } else if ("Skills".equals(submodelIdShort)) {
                            tripleCount += parseSkillsSubmodel(
                                submodel, idShort, model, rdfType, rdfsLabel, skillClass, details);
                        }
                    }
                }

                // Parse asset information for Service identification
                JsonNode assetInfo = shell.path("assetInformation");
                if (assetInfo.isObject()) {
                    tripleCount += parseAssetIdentification(
                        assetInfo, shell, model, rdfType, rdfsLabel, details);
                }
            }

            // Parse top-level submodels (ProcessValues etc.)
            JsonNode topSubmodels = root.path("submodels");
            if (topSubmodels.isArray()) {
                for (JsonNode submodel : topSubmodels) {
                    String submodelIdShort = submodel.path("idShort").asText("");
                    if ("ProcessValues".equals(submodelIdShort)) {
                        tripleCount += parseProcessValuesSubmodel(
                            submodel, model, rdfType, rdfsLabel, connectorClass, details);
                    }
                }
            }

            // Wire service → binding and service → connector via hasBinding/hasConnector
            Resource firstBinding = model.getResource(INST_NS + "CB-AAS-0");
            Resource firstConnector = model.getResource(INST_NS + "DC-AAS-0");
            Resource serviceRes = model.getResource(INST_NS + "SVC-AAS-0");
            if (serviceRes != null && model.containsResource(serviceRes)) {
                if (firstBinding != null && model.containsResource(firstBinding)) {
                    serviceRes.addProperty(hasBinding, firstBinding);
                    tripleCount++;
                }
                if (firstConnector != null && model.containsResource(firstConnector)) {
                    serviceRes.addProperty(hasConnector, firstConnector);
                    tripleCount++;
                }
            }

            // Wire skill → service via isImplementedBy
            if (serviceRes != null && model.containsResource(serviceRes)) {
                for (int i = 0; i < 10; i++) {
                    Resource skillRes = model.getResource(INST_NS + "SK-AAS-" + i);
                    if (skillRes != null && model.containsResource(skillRes)) {
                        skillRes.addProperty(isImplementedBy, serviceRes);
                        tripleCount++;
                    }
                }
            }

            String summary = "Parsed AAS: " + tripleCount + " triples from "
                + shells.size() + " administration shell(s)";

            log.info("AAS parse completed: {}", summary);

            return new ParseResult("AAS", tripleCount, details, summary, model);

        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Failed to parse AAS JSON: " + e.getMessage(), e);
        }
    }

    private int parseCommunicationSubmodel(JsonNode submodel, Model model,
                                            Property rdfType, Property rdfsLabel,
                                            Resource bindingClass, Property protocol,
                                            Property accessMode, Property securityMode,
                                            List<String> details) {
        int tripleCount = 0;

        String protocolVal = "";
        String endpointVal = "";
        String accessModeVal = "read-only";
        String secModeVal = "None";

        JsonNode properties = submodel.path("properties");
        if (properties.isArray()) {
            for (JsonNode prop : properties) {
                String idShort = prop.path("idShort").asText("");
                String value = prop.path("value").asText("");
                switch (idShort) {
                    case "Protocol" -> protocolVal = value;
                    case "EndpointUrl" -> endpointVal = value;
                    case "AccessMode" -> accessModeVal = value;
                    case "SecurityMode" -> secModeVal = value;
                    default -> { }
                }
            }
        }

        String bindingLocalName = "CB-AAS-0";
        Resource bindingRes = model.createResource(INST_NS + bindingLocalName);
        bindingRes.addProperty(rdfType, bindingClass);
        bindingRes.addProperty(rdfsLabel, "AAS binding 0");
        bindingRes.addProperty(protocol, protocolVal);
        bindingRes.addProperty(accessMode, accessModeVal);
        bindingRes.addProperty(securityMode, secModeVal);
        tripleCount += 5;

        details.add("CommunicationBinding: " + INST_NS + bindingLocalName
            + " (endpoint=" + endpointVal
            + ", protocol=" + protocolVal
            + ", securityMode=" + secModeVal + ")");

        return tripleCount;
    }

    private int parseSkillsSubmodel(JsonNode submodel, String aasIdShort,
                                     Model model, Property rdfType, Property rdfsLabel,
                                     Resource skillClass, List<String> details) {
        int tripleCount = 0;
        int skillIndex = 0;

        JsonNode elements = submodel.path("submodelElements");
        if (elements.isArray()) {
            for (JsonNode element : elements) {
                String idShort = element.path("idShort").asText("UnknownSkill");

                String engUnitVal = "";
                String minVal = "";
                String maxVal = "";

                JsonNode props = element.path("properties");
                if (props.isArray()) {
                    for (JsonNode prop : props) {
                        String propIdShort = prop.path("idShort").asText("");
                        String value = prop.path("value").asText("");
                        switch (propIdShort) {
                            case "EngineeringUnit" -> engUnitVal = value;
                            case "NominalRangeMin" -> minVal = value;
                            case "NominalRangeMax" -> maxVal = value;
                            default -> { }
                        }
                    }
                }

                String skillLocalName = "SK-AAS-" + skillIndex;
                Resource skillRes = model.createResource(INST_NS + skillLocalName);
                skillRes.addProperty(rdfType, skillClass);
                skillRes.addProperty(rdfsLabel, idShort);
                tripleCount += 2;

                details.add("Skill: " + INST_NS + skillLocalName
                    + " [" + idShort + "]"
                    + " (unit=" + engUnitVal
                    + ", range=[" + minVal + "," + maxVal + "])");

                skillIndex++;
            }
        }

        return tripleCount;
    }

    private int parseAssetIdentification(JsonNode assetInfo, JsonNode shell,
                                          Model model, Property rdfType, Property rdfsLabel,
                                          List<String> details) {
        int tripleCount = 0;

        String globalAssetId = assetInfo.path("globalAssetId").asText("");
        JsonNode admin = shell.path("administration");
        String version = admin.path("version").asText("");
        String revision = admin.path("revision").asText("");

        // Create a Service resource representing the AAS device
        String serviceLocalName = "SVC-AAS-0";
        Resource serviceRes = model.createResource(INST_NS + serviceLocalName);
        serviceRes.addProperty(rdfType, model.createResource(SDA_NS + "Service"));
        serviceRes.addProperty(rdfsLabel, globalAssetId);
        tripleCount += 2;

        details.add("Identification: assetId=" + globalAssetId
            + ", version=" + version + "." + revision);

        return tripleCount;
    }

    private int parseProcessValuesSubmodel(JsonNode submodel, Model model,
                                            Property rdfType, Property rdfsLabel,
                                            Resource connectorClass, List<String> details) {
        int tripleCount = 0;

        String connectorLocalName = "DC-AAS-0";
        Resource connectorRes = model.createResource(INST_NS + connectorLocalName);
        connectorRes.addProperty(rdfType, connectorClass);
        connectorRes.addProperty(rdfsLabel, "AAS connector 0");
        tripleCount += 2;

        // Extract process value names as details
        JsonNode elements = submodel.path("submodelElements");
        if (elements.isArray()) {
            List<String> valueNames = new ArrayList<>();
            for (JsonNode element : elements) {
                String idShort = element.path("idShort").asText("");
                if (!idShort.isBlank()) {
                    valueNames.add(idShort);
                }
            }
            if (!valueNames.isEmpty()) {
                details.add("DataConnector: " + INST_NS + connectorLocalName
                    + " (values=" + String.join(", ", valueNames) + ")");
            }
        } else {
            details.add("DataConnector: " + INST_NS + connectorLocalName);
        }

        return tripleCount;
    }
}
