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
import java.util.List;

/**
 * Parses VDI/VDE/NAMUR 2658 MTP AML XML files into SDA semantic triples.
 * Extracts Skills from {@code <Service>} blocks, CommunicationBindings from
 * {@code <OpcUaServer>} blocks, and DataConnectors from {@code <OpcUaNode>} blocks.
 * Produces a Jena Model containing instantiated ABox triples.
 */
@Component
public class MTPAmlParser implements Parser {

    private static final Logger log = LoggerFactory.getLogger(MTPAmlParser.class);

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

            Model model = ModelFactory.createDefaultModel();
            model.setNsPrefix("sda", SDA_NS);
            model.setNsPrefix("inst", INST_NS);

            Property rdfType = model.createProperty(RDF_TYPE);
            Property rdfsLabel = model.createProperty(RDFS_LABEL);
            Resource skillClass = model.createResource(SDA_NS + "Skill");
            Resource bindingClass = model.createResource(SDA_NS + "CommunicationBinding");
            Resource connectorClass = model.createResource(SDA_NS + "DataConnector");
            Property engineeringUnit = model.createProperty(SDA_NS + "engineeringUnit");
            Property nominalRangeMin = model.createProperty(SDA_NS + "nominalRange_min");
            Property nominalRangeMax = model.createProperty(SDA_NS + "nominalRange_max");
            Property protocol = model.createProperty(SDA_NS + "protocol");
            Property accessMode = model.createProperty(SDA_NS + "accessMode");
            Property securityMode = model.createProperty(SDA_NS + "securityMode");
            Property isImplementedBy = model.createProperty(SDA_NS + "isImplementedBy");
            Property hasBinding = model.createProperty(SDA_NS + "hasBinding");
            Property hasConnector = model.createProperty(SDA_NS + "hasConnector");
            Property informationModel = model.createProperty(SDA_NS + "informationModel");

            List<String> details = new ArrayList<>();
            int tripleCount = 0;

            // Parse <Service> blocks into sda:Skill instances
            NodeList serviceNodes = doc.getElementsByTagName("Service");
            List<Resource> skillResources = new ArrayList<>();
            for (int i = 0; i < serviceNodes.getLength(); i++) {
                Element service = (Element) serviceNodes.item(i);
                String name = service.getAttribute("Name");
                if (name == null || name.isBlank()) {
                    name = "UnknownService_" + i;
                }

                String engineeringUnitVal = "";
                String nominalRangeMinVal = "";
                String nominalRangeMaxVal = "";

                NodeList paramNodes = service.getElementsByTagName("Parameter");
                for (int j = 0; j < paramNodes.getLength(); j++) {
                    Element param = (Element) paramNodes.item(j);
                    String unit = param.getAttribute("EngineeringUnit");
                    if (unit != null && !unit.isBlank()) {
                        engineeringUnitVal = unit;
                    }
                    String minVal = param.getAttribute("NominalRangeMin");
                    if (minVal != null && !minVal.isBlank()) {
                        nominalRangeMinVal = minVal;
                    }
                    String maxVal = param.getAttribute("NominalRangeMax");
                    if (maxVal != null && !maxVal.isBlank()) {
                        nominalRangeMaxVal = maxVal;
                    }
                }

                String skillLocalName = "SK-" + name;
                Resource skillRes = model.createResource(INST_NS + skillLocalName);
                skillRes.addProperty(rdfType, skillClass);
                skillRes.addProperty(rdfsLabel, name);
                if (!engineeringUnitVal.isBlank()) {
                    skillRes.addProperty(engineeringUnit, engineeringUnitVal);
                }
                if (!nominalRangeMinVal.isBlank()) {
                    skillRes.addProperty(nominalRangeMin, model.createTypedLiteral(
                        Double.parseDouble(nominalRangeMinVal)));
                }
                if (!nominalRangeMaxVal.isBlank()) {
                    skillRes.addProperty(nominalRangeMax, model.createTypedLiteral(
                        Double.parseDouble(nominalRangeMaxVal)));
                }
                tripleCount += 3;
                skillResources.add(skillRes);

                details.add("Skill: " + INST_NS + skillLocalName + " (unit=" + engineeringUnitVal
                    + ", range=[" + nominalRangeMinVal + "," + nominalRangeMaxVal + "])");
            }

            // Parse <Communication><OpcUaServer> into sda:CommunicationBinding triples
            List<Resource> serviceResources = new ArrayList<>();
            NodeList opcUaServerNodes = doc.getElementsByTagName("OpcUaServer");
            for (int i = 0; i < opcUaServerNodes.getLength(); i++) {
                Element opcUaServer = (Element) opcUaServerNodes.item(i);
                String endpoint = opcUaServer.getAttribute("Endpoint");
                String secMode = opcUaServer.getAttribute("SecurityMode");
                if (secMode == null || secMode.isBlank()) {
                    secMode = "None";
                }

                String bindingLocalName = "CB-MTP-" + i;
                Resource bindingRes = model.createResource(INST_NS + bindingLocalName);
                bindingRes.addProperty(rdfType, bindingClass);
                bindingRes.addProperty(rdfsLabel, "MTP binding " + i);
                bindingRes.addProperty(protocol, "OPC-UA");
                bindingRes.addProperty(accessMode, "read-only");
                bindingRes.addProperty(securityMode, secMode);

                // Wire skill → binding via isImplementedBy (skill is implemented by service)
                // and service uses binding. We'll create a per-service for each skill.
                String serviceLocalName = "SVC-MTP-Skill" + i;
                Resource serviceRes = model.createResource(INST_NS + serviceLocalName);
                serviceRes.addProperty(rdfType, model.createResource(SDA_NS + "Service"));
                serviceRes.addProperty(rdfsLabel, "MTP Service " + i);
                serviceRes.addProperty(hasBinding, bindingRes);
                if (i < skillResources.size()) {
                    skillResources.get(i).addProperty(isImplementedBy, serviceRes);
                }

                tripleCount += 5; // rdf:type + label + protocol + accessMode + hasBinding + isImplementedBy
                serviceResources.add(serviceRes);

                details.add("CommunicationBinding: " + INST_NS + bindingLocalName
                    + " (endpoint=" + endpoint
                    + ", protocol=OPC-UA"
                    + ", securityMode=" + secMode + ")");
            }

            // Parse <DataAssembly><OpcUaNode> into sda:DataConnector triples
            NodeList opcUaNodeNodes = doc.getElementsByTagName("OpcUaNode");
            for (int i = 0; i < opcUaNodeNodes.getLength(); i++) {
                Element opcUaNode = (Element) opcUaNodeNodes.item(i);
                String nodeId = opcUaNode.getAttribute("NodeId");

                String connectorLocalName = "DC-MTP-" + i;
                Resource connectorRes = model.createResource(INST_NS + connectorLocalName);
                connectorRes.addProperty(rdfType, connectorClass);
                connectorRes.addProperty(rdfsLabel, "MTP connector " + i);
                connectorRes.addProperty(informationModel, "OPC-UA");

                // Wire service → connector via hasConnector
                if (i < serviceResources.size()) {
                    serviceResources.get(i).addProperty(hasConnector, connectorRes);
                }

                tripleCount += 3; // rdf:type + label + informationModel

                details.add("DataConnector: " + INST_NS + connectorLocalName
                    + " (nodeId=" + nodeId + ")");
            }

            String summary = "Parsed MTP AML: " + tripleCount + " triples from "
                + serviceNodes.getLength() + " services, "
                + opcUaServerNodes.getLength() + " OPC UA servers, "
                + opcUaNodeNodes.getLength() + " OPC UA nodes";

            log.info("MTP AML parse completed: {}", summary);

            return new ParseResult("MTP-AML", tripleCount, details, summary, model);

        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Failed to parse MTP AML: " + e.getMessage(), e);
        }
    }
}
