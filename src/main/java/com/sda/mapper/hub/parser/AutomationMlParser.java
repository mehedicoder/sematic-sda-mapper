package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.AutomationMLEntity;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses a generic AutomationML (CAEX) description into the canonical asset model.
 *
 * <p>MTP AML is a profile of AutomationML handled by {@link MtpModelParser}; this
 * parser targets plain AutomationML/CAEX documents (no MTP markers) and lifts
 * each {@code InternalElement} and {@code ExternalInterface} encountered in an
 * {@code InstanceHierarchy} into an {@link AutomationMLEntity}.</p>
 */
@Component
public class AutomationMlParser implements CanonicalModelParser {

    private static final Logger log = LoggerFactory.getLogger(AutomationMlParser.class);

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.AUTOMATION_ML;
    }

    @Override
    public CanonicalAssetModel parse(InputStream input, String fileName) {
        if (input == null) {
            throw new ParsingException(fileName, "No content to parse");
        }
        byte[] bytes;
        try {
            bytes = input.readAllBytes();
        } catch (IOException e) {
            throw new ParsingException(fileName, "Could not read content", e);
        }
        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new ParsingException(fileName, "Invalid XML: " + e.getMessage(), e);
        }
        Element root = doc.getDocumentElement();
        if (root == null || !isCaex(root)) {
            throw new ParsingException(fileName,
                "Root element is not a CAEX/AutomationML document");
        }
        CanonicalAssetModel model = new CanonicalAssetModel();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element
                    && localName(element).equals("InstanceHierarchy")) {
                walk(element, model, fileName);
            }
        }
        log.info("Parsed AutomationML file '{}' into {} canonical entities",
            fileName, model.allEntities().size());
        return model;
    }

    private boolean isCaex(Element root) {
        String local = localName(root).toLowerCase();
        return local.equals("caexfile") || local.equals("caex");
    }

    private void walk(Element element, CanonicalAssetModel model, String fileName) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element child)) {
                continue;
            }
            String local = localName(child);
            if (local.equals("InternalElement")) {
                model.addEntity(toEntity(child, "InternalElement", fileName));
                walk(child, model, fileName);
            } else if (local.equals("ExternalInterface")) {
                model.addEntity(toEntity(child, "ExternalInterface", fileName));
            }
        }
    }

    private AutomationMLEntity toEntity(Element element, String type, String fileName) {
        String id = attribute(element, "ID", attribute(element, "Name", type + "-unknown"));
        String name = attribute(element, "Name", id);
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("caexElementType", type);
        NodeList roleRequirements = element.getElementsByTagNameNS("*", "RoleRequirements");
        if (roleRequirements.getLength() > 0) {
            attrs.put("RefRoleClassPath",
                attribute((Element) roleRequirements.item(0), "RefRoleClassPath", ""));
        }
        String base = attribute(element, "RefBaseSystemUnitPath", "");
        if (!base.isEmpty()) {
            attrs.put("RefBaseSystemUnitPath", base);
        }
        NodeList attributes = element.getElementsByTagNameNS("*", "Attribute");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attribute = (Element) attributes.item(i);
            String attributeName = attribute(attribute, "Name", "");
            if (!attributeName.isEmpty()) {
                attrs.put(attributeName, attribute(attribute, "Value", attribute.getTextContent()));
            }
        }
        return new AutomationMLEntity("AutomationML", id, name, fileName, attrs);
    }

    private static String localName(Element element) {
        String local = element.getLocalName();
        return local != null ? local : element.getTagName();
    }

    private static String attribute(Element element, String name, String fallback) {
        String value = element.getAttribute(name);
        return (value == null || value.isEmpty()) ? fallback : value;
    }
}
