package com.sda.mapper.imports;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects missing semantic information in parsed ontology models.
 * Inspects each entity against the CSS-SDA hierarchy expectations and categorizes gaps.
 */
@Component
public class MissingInformationDetector {

    private static final Logger log = LoggerFactory.getLogger(MissingInformationDetector.class);

    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String CSS_NS = "http://www.example.org/css#";
    private static final String INST_NS = "http://www.example.org/instance#";

    private final OntologyModelLoader ontologyModelLoader;

    public MissingInformationDetector(OntologyModelLoader ontologyModelLoader) {
        this.ontologyModelLoader = ontologyModelLoader;
    }

    /**
     * Detects missing information in the given model.
     *
     * @param model the merged model containing newly parsed entities
     * @return list of missing items categorized by type
     */
    public List<MissingItem> detectMissing(Model model) {
        List<MissingItem> items = new ArrayList<>();

        detectMissingCommunicationBindings(model, items);
        detectMissingCapabilities(model, items);
        detectMissingDeploymentContexts(model, items);
        detectMissingSemanticIds(model, items);
        detectMissingEngineeringUnits(model, items);
        detectMissingDeviceMetadata(model, items);

        log.info("Detected {} missing items across {} categories",
            items.size(), countCategories(items));
        return items;
    }

    private void detectMissingCommunicationBindings(Model model, List<MissingItem> items) {
        Property usesBinding = model.createProperty(SDA_NS + "usesBinding");
        var services = model.listSubjectsWithProperty(RDF.type,
            model.createResource(CSS_NS + "Service")).toList();

        for (Resource service : services) {
            if (!service.hasProperty(usesBinding)) {
                String label = getLabel(service, model);
                items.add(new MissingItem(
                    MissingCategory.MISSING_COMMUNICATION_BINDING,
                    service.getURI(), label, "Service",
                    "sda:usesBinding",
                    "Service '" + label + "' has no CommunicationBinding",
                    true
                ));
            }
        }
    }

    private void detectMissingCapabilities(Model model, List<MissingItem> items) {
        Property isImplementedBy = model.createProperty(SDA_NS + "isImplementedBy");
        Property realisedBy = model.createProperty(SDA_NS + "realisedBy");
        var services = model.listSubjectsWithProperty(RDF.type,
            model.createResource(CSS_NS + "Service")).toList();

        for (Resource service : services) {
            boolean hasCapability = model.listSubjectsWithProperty(realisedBy,
                    model.listSubjectsWithProperty(isImplementedBy, service)
                        .toList().isEmpty() ? null : service)
                .hasNext();

            if (!hasCapability) {
                boolean skillImplements = model.listSubjectsWithProperty(isImplementedBy, service).hasNext();
                if (!skillImplements) {
                    String label = getLabel(service, model);
                    items.add(new MissingItem(
                        MissingCategory.MISSING_CAPABILITY,
                        service.getURI(), label, "Service",
                        "capability chain",
                        "Service '" + label + "' is not linked to any Capability via Skill",
                        true
                    ));
                }
            }
        }
    }

    private void detectMissingDeploymentContexts(Model model, List<MissingItem> items) {
        Property isDeployedBy = model.createProperty(SDA_NS + "isDeployedBy");
        Property sil = model.createProperty(SDA_NS + "safetyIntegrityLevel");
        Property zone = model.createProperty(SDA_NS + "networkZone");
        Property determ = model.createProperty(SDA_NS + "determinismClass");
        var services = model.listSubjectsWithProperty(RDF.type,
            model.createResource(CSS_NS + "Service")).toList();

        for (Resource service : services) {
            if (!service.hasProperty(isDeployedBy)) {
                String label = getLabel(service, model);
                items.add(new MissingItem(
                    MissingCategory.MISSING_DEPLOYMENT_CONTEXT,
                    service.getURI(), label, "Service",
                    "sda:isDeployedBy",
                    "Service '" + label + "' has no DeploymentContext",
                    true
                ));
            } else {
                Resource deploy = service.getPropertyResourceValue(isDeployedBy);
                boolean complete = deploy.hasProperty(sil) && deploy.hasProperty(zone) && deploy.hasProperty(determ);
                if (!complete) {
                    String label = getLabel(deploy, model);
                    List<String> missing = new ArrayList<>();
                    if (!deploy.hasProperty(sil)) missing.add("safetyIntegrityLevel");
                    if (!deploy.hasProperty(zone)) missing.add("networkZone");
                    if (!deploy.hasProperty(determ)) missing.add("determinismClass");
                    items.add(new MissingItem(
                        MissingCategory.MISSING_DEPLOYMENT_CONTEXT,
                        deploy.getURI(), label, "DeploymentContext",
                        "DeploymentContext properties",
                        "DeploymentContext '" + label + "' is missing: " + String.join(", ", missing),
                        true
                    ));
                }
            }
        }
    }

    private void detectMissingSemanticIds(Model model, List<MissingItem> items) {
        Property hasParameter = model.createProperty(SDA_NS + "hasParameter");
        Property semanticID = model.createProperty(SDA_NS + "semanticID");
        var skills = model.listSubjectsWithProperty(RDF.type,
            model.createResource(CSS_NS + "Skill")).toList();

        for (Resource skill : skills) {
            var params = skill.listProperties(hasParameter).toList();
            for (var paramStmt : params) {
                if (paramStmt.getObject().isResource()) {
                    Resource param = paramStmt.getObject().asResource();
                    if (!param.hasProperty(semanticID)) {
                        String label = getLabel(param, model);
                        items.add(new MissingItem(
                            MissingCategory.MISSING_SEMANTIC_ID,
                            param.getURI(), label, "SkillParameter",
                            "sda:semanticID",
                            "Parameter '" + label + "' has no IEC CDD IRDI semantic identifier",
                            true
                        ));
                    }
                }
            }
        }
    }

    private void detectMissingEngineeringUnits(Model model, List<MissingItem> items) {
        Property hasParameter = model.createProperty(SDA_NS + "hasParameter");
        Property engineeringUnit = model.createProperty(SDA_NS + "engineeringUnit");
        var skills = model.listSubjectsWithProperty(RDF.type,
            model.createResource(CSS_NS + "Skill")).toList();

        for (Resource skill : skills) {
            var params = skill.listProperties(hasParameter).toList();
            for (var paramStmt : params) {
                if (paramStmt.getObject().isResource()) {
                    Resource param = paramStmt.getObject().asResource();
                    if (!param.hasProperty(engineeringUnit)) {
                        String label = getLabel(param, model);
                        items.add(new MissingItem(
                            MissingCategory.MISSING_ENGINEERING_UNIT,
                            param.getURI(), label, "SkillParameter",
                            "sda:engineeringUnit",
                            "Parameter '" + label + "' has no UCUM engineering unit",
                            true
                        ));
                    }
                }
            }
        }
    }

    private void detectMissingDeviceMetadata(Model model, List<MissingItem> items) {
        Property vendor = model.createProperty(SDA_NS + "vendor");
        Property modelProp = model.createProperty(SDA_NS + "model");
        Property version = model.createProperty(SDA_NS + "version");
        var devices = model.listSubjectsWithProperty(RDF.type,
            model.createResource(SDA_NS + "FieldDevice")).toList();
        var mtpModules = model.listSubjectsWithProperty(RDF.type,
            model.createResource(SDA_NS + "MTPModule")).toList();

        List<Resource> allDevices = new ArrayList<>();
        allDevices.addAll(devices);
        allDevices.addAll(mtpModules);

        for (Resource device : allDevices) {
            boolean hasVendor = device.hasProperty(vendor);
            boolean hasModel = device.hasProperty(modelProp);
            boolean hasVersion = device.hasProperty(version);

            if (!hasVendor || !hasModel || !hasVersion) {
                String label = getLabel(device, model);
                List<String> missing = new ArrayList<>();
                if (!hasVendor) missing.add("vendor");
                if (!hasModel) missing.add("model");
                if (!hasVersion) missing.add("version");
                items.add(new MissingItem(
                    MissingCategory.MISSING_DEVICE_METADATA,
                    device.getURI(), label, "Device",
                    "device metadata",
                    "Device '" + label + "' is missing: " + String.join(", ", missing),
                    true
                ));
            }
        }
    }

    private String getLabel(Resource resource, Model model) {
        var stmt = model.getProperty(resource, RDFS.label);
        if (stmt != null && stmt.getString() != null) {
            return stmt.getString();
        }
        return resource.getLocalName();
    }

    private int countCategories(List<MissingItem> items) {
        return (int) items.stream().map(MissingItem::category).distinct().count();
    }
}
