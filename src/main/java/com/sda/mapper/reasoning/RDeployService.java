package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Java equivalent of the SWRL R-Deploy rule from the paper (Table I).
 * <p>
 * After a SPARQL match identifies a valid Service for a Requirement,
 * this service asserts the complete runtime binding tuple
 * (boundService, boundBinding, boundConnector, boundContext)
 * on the Requirement and updates its lifecycleStatus to "Matched".
 * <p>
 * Paper reference: R-Deploy SWRL rule — "Asserts binding relationships
 * and updates lifecycleStatus to Matched".
 */
@Service
public class RDeployService {

    private static final Logger log = LoggerFactory.getLogger(RDeployService.class);

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "https://example.org/ontology/sda#";

    private final OntologyModelLoader ontologyModelLoader;

    public RDeployService(OntologyModelLoader ontologyModelLoader) {
        this.ontologyModelLoader = ontologyModelLoader;
    }

    /**
     * Executes the R-Deploy rule: asserts bound properties on the matched
     * Requirement and transitions lifecycleStatus to "Matched".
     *
     * @param requirementId the requirement ID (e.g. "REQ-001")
     * @param serviceName   rdfs:label of the matched Service
     * @param bindingLabel  rdfs:label of the matched CommunicationBinding
     * @param connectorLabel rdfs:label of the matched DataConnector
     * @param deployLabel   rdfs:label of the matched DeploymentContext
     */
    public void execute(String requirementId, String serviceName,
                        String bindingLabel, String connectorLabel,
                        String deployLabel) {
        Model model = ontologyModelLoader.getModel();

        Resource reqResource = model.createResource(INST_NS + requirementId);

        Property boundServiceProp = model.createProperty(SDA_NS + "boundService");
        Property boundBindingProp = model.createProperty(SDA_NS + "boundBinding");
        Property boundConnectorProp = model.createProperty(SDA_NS + "boundConnector");
        Property boundContextProp = model.createProperty(SDA_NS + "boundContext");
        Property matchedCandidateProp = model.createProperty(SDA_NS + "matchedCandidate");
        Property lifecycleStatusProp = model.createProperty(SDA_NS + "lifecycleStatus");

        Resource serviceResource = findResourceByLabel(model, SDA_NS + "Service", serviceName);
        Resource bindingResource = findResourceByLabel(model, SDA_NS + "CommunicationBinding", bindingLabel);
        Resource connectorResource = findResourceByLabel(model, SDA_NS + "DataConnector", connectorLabel);
        Resource deployResource = findResourceByLabel(model, SDA_NS + "DeploymentContext", deployLabel);

        if (serviceResource != null) {
            reqResource.addProperty(boundServiceProp, serviceResource);
            reqResource.addProperty(matchedCandidateProp, serviceResource);
            log.debug("R-Deploy: boundService({}) = {}", requirementId, serviceName);
        }

        if (bindingResource != null) {
            reqResource.addProperty(boundBindingProp, bindingResource);
            log.debug("R-Deploy: boundBinding({}) = {}", requirementId, bindingLabel);
        }

        if (connectorResource != null) {
            reqResource.addProperty(boundConnectorProp, connectorResource);
            log.debug("R-Deploy: boundConnector({}) = {}", requirementId, connectorLabel);
        }

        if (deployResource != null) {
            reqResource.addProperty(boundContextProp, deployResource);
            log.debug("R-Deploy: boundContext({}) = {}", requirementId, deployLabel);
        }

        reqResource.removeAll(lifecycleStatusProp);
        reqResource.addProperty(lifecycleStatusProp, ResourceFactory.createStringLiteral("Matched"));
        log.info("R-Deploy: lifecycleStatus({}) = Matched", requirementId);
    }

    /**
     * Reads the current lifecycleStatus of a requirement.
     *
     * @param requirementId the requirement ID
     * @return current lifecycle status, or "Unknown" if not found
     */
    public String getLifecycleStatus(String requirementId) {
        Model model = ontologyModelLoader.getModel();
        Resource reqResource = model.createResource(INST_NS + requirementId);
        Property lifecycleStatusProp = model.createProperty(SDA_NS + "lifecycleStatus");

        var stmt = reqResource.getProperty(lifecycleStatusProp);
        if (stmt != null && stmt.getString() != null) {
            return stmt.getString();
        }
        return "Unknown";
    }

    /**
     * Checks whether a requirement has been matched (has bound* properties).
     *
     * @param requirementId the requirement ID
     * @return true if the requirement has a boundService
     */
    public boolean isMatched(String requirementId) {
        Model model = ontologyModelLoader.getModel();
        Resource reqResource = model.createResource(INST_NS + requirementId);
        Property boundServiceProp = model.createProperty(SDA_NS + "boundService");

        return reqResource.hasProperty(boundServiceProp);
    }

    private Resource findResourceByLabel(Model model, String classUri, String label) {
        if (label == null) return null;
        var stmts = model.listSubjectsWithProperty(
            org.apache.jena.vocabulary.RDFS.label, label);
        while (stmts.hasNext()) {
            Resource r = stmts.next();
            if (r.hasProperty(org.apache.jena.vocabulary.RDF.type,
                model.createResource(classUri))) {
                return r;
            }
        }
        return null;
    }
}
