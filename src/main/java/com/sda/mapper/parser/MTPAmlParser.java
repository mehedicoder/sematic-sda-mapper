package com.sda.mapper.parser;

import com.sda.mapper.error.ParserException;
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
 */
@Component
public class MTPAmlParser implements Parser {

    private static final Logger log = LoggerFactory.getLogger(MTPAmlParser.class);

    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String CSS_NS = "http://www.example.org/css#";

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

            List<String> details = new ArrayList<>();
            int tripleCount = 0;

            // Parse <Service> blocks into css:Skill instances
            NodeList serviceNodes = doc.getElementsByTagName("Service");
            for (int i = 0; i < serviceNodes.getLength(); i++) {
                Element service = (Element) serviceNodes.item(i);
                String name = service.getAttribute("Name");
                if (name == null || name.isBlank()) {
                    name = service.getAttribute("Name");
                }
                if (name == null || name.isBlank()) {
                    name = "UnknownService_" + i;
                }

                String engineeringUnit = "";
                String nominalRangeMin = "";
                String nominalRangeMax = "";

                NodeList paramNodes = service.getElementsByTagName("Parameter");
                for (int j = 0; j < paramNodes.getLength(); j++) {
                    Element param = (Element) paramNodes.item(j);
                    String unit = param.getAttribute("EngineeringUnit");
                    if (unit != null && !unit.isBlank()) {
                        engineeringUnit = unit;
                    }
                    String minVal = param.getAttribute("NominalRangeMin");
                    if (minVal != null && !minVal.isBlank()) {
                        nominalRangeMin = minVal;
                    }
                    String maxVal = param.getAttribute("NominalRangeMax");
                    if (maxVal != null && !maxVal.isBlank()) {
                        nominalRangeMax = maxVal;
                    }
                }

                String skillUri = CSS_NS + name;
                details.add("Skill: " + skillUri + " (unit=" + engineeringUnit
                    + ", range=[" + nominalRangeMin + "," + nominalRangeMax + "])");
                tripleCount += 3; // rdf:type + engineeringUnit + nominalRange
            }

            // Parse <Communication><OpcUaServer> into sda:CommunicationBinding triples
            NodeList opcUaServerNodes = doc.getElementsByTagName("OpcUaServer");
            for (int i = 0; i < opcUaServerNodes.getLength(); i++) {
                Element opcUaServer = (Element) opcUaServerNodes.item(i);
                String endpoint = opcUaServer.getAttribute("Endpoint");
                String securityMode = opcUaServer.getAttribute("SecurityMode");
                if (securityMode == null || securityMode.isBlank()) {
                    securityMode = "None";
                }

                String bindingUri = SDA_NS + "CommunicationBinding_" + i;
                details.add("CommunicationBinding: " + bindingUri
                    + " (endpoint=" + endpoint
                    + ", protocol=OPC-UA"
                    + ", securityMode=" + securityMode + ")");
                tripleCount += 3; // rdf:type + endpoint + protocol
            }

            // Parse <DataAssembly><OpcUaNode> into sda:DataConnector triples
            NodeList opcUaNodeNodes = doc.getElementsByTagName("OpcUaNode");
            for (int i = 0; i < opcUaNodeNodes.getLength(); i++) {
                Element opcUaNode = (Element) opcUaNodeNodes.item(i);
                String nodeId = opcUaNode.getAttribute("NodeId");

                String connectorUri = SDA_NS + "DataConnector_" + i;
                details.add("DataConnector: " + connectorUri
                    + " (nodeId=" + nodeId + ")");
                tripleCount += 2; // rdf:type + nodeId
            }

            String summary = "Parsed MTP AML: " + tripleCount + " triples from "
                + serviceNodes.getLength() + " services, "
                + opcUaServerNodes.getLength() + " OPC UA servers, "
                + opcUaNodeNodes.getLength() + " OPC UA nodes";

            log.info("MTP AML parse completed: {}", summary);

            return new ParseResult("MTP-AML", tripleCount, details, summary);

        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Failed to parse MTP AML: " + e.getMessage(), e);
        }
    }
}
