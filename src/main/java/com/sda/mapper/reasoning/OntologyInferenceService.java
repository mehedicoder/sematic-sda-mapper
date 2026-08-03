package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs lightweight OWL inference on the CSS-SDA ontology model using
 * the built-in OWL reasoner via {@link ReasonerRegistry#getOWLReasoner()}.
 * <p>
 * Infers new {@code sda:OfferedCapability} instances from {@code css:Skill}
 * instances that carry {@code sda:engineeringUnit}, {@code sda:nominalRange_min},
 * and {@code sda:nominalRange_max} data properties, without overwriting existing
 * explicit triples.
 */
@Service
@ConfigurationProperties(prefix = "app.ontology.inference")
public class OntologyInferenceService {

    private static final Logger log = LoggerFactory.getLogger(OntologyInferenceService.class);

    private static final String SDA_NS = "https://example.org/ontology/sda#";
    private static final String INST_NS = "http://www.example.org/instance#";

    private final OntologyModelLoader ontologyModelLoader;
    private boolean enabled = true;
    private InfModel inferredModel;
    private long baseTripleCount;
    private List<InferredCapability> inferredCapabilities = List.of();

    public OntologyInferenceService(OntologyModelLoader ontologyModelLoader) {
        this.ontologyModelLoader = ontologyModelLoader;
    }

    @PostConstruct
    public void init() {
        if (enabled && ontologyModelLoader.isLoaded()) {
            runInference();
        }
    }

    /**
     * Runs OWL inference on the loaded ontology model
     * and derives OfferedCapability instances from Skill parameters.
     * Skipped if inference is disabled via configuration.
     */
    public void runInference() {
        if (!enabled) {
            log.info("Inference disabled by configuration");
            inferredCapabilities = List.of();
            return;
        }
        if (!ontologyModelLoader.isLoaded()) {
            log.warn("Cannot run inference: ontology not loaded");
            return;
        }

        Model baseModel = ontologyModelLoader.getModel();
        baseTripleCount = baseModel.size();

        Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
        inferredModel = ModelFactory.createInfModel(reasoner, baseModel);

        log.info("OWL inference model created: {} triples (base: {})",
            inferredModel.size(), baseTripleCount);

        inferredCapabilities = deriveCapabilitiesFromSkills();
        log.info("Inferred {} capabilities from Skill parameters", inferredCapabilities.size());
    }

    /**
     * Returns the model containing both explicit and inferred triples.
     *
     * @return the inferred Jena model
     */
    public Model getInferredModel() {
        if (inferredModel == null) {
            return ontologyModelLoader.getModel();
        }
        return inferredModel;
    }

    /**
     * Returns the count of triples added by inference (inferred minus base).
     *
     * @return number of inferred triples
     */
    public int getInferredTripleCount() {
        if (inferredModel == null) {
            return 0;
        }
        return (int) (inferredModel.size() - baseTripleCount);
    }

    /**
     * Returns capabilities derived from Skill instances that carry
     * engineering unit and nominal range data properties.
     *
     * @return list of inferred capabilities
     */
    public List<InferredCapability> getInferredCapabilities() {
        return inferredCapabilities;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private List<InferredCapability> deriveCapabilitiesFromSkills() {
        Model model = ontologyModelLoader.getModel();
        List<InferredCapability> result = new ArrayList<>();

        var skills = model.listSubjectsWithProperty(
            org.apache.jena.vocabulary.RDF.type,
            model.createResource(SDA_NS + "Skill")).toList();

        for (Resource skill : skills) {
            var engUnitStmt = skill.getProperty(model.createProperty(SDA_NS + "engineeringUnit"));
            var minStmt = skill.getProperty(model.createProperty(SDA_NS + "nominalRange_min"));
            var maxStmt = skill.getProperty(model.createProperty(SDA_NS + "nominalRange_max"));

            if (engUnitStmt == null || minStmt == null || maxStmt == null) {
                continue;
            }

            String engUnit = engUnitStmt.getString();
            double minRange = minStmt.getDouble();
            double maxRange = maxStmt.getDouble();
            String skillLocalName = getLocalName(skill);

            // Find if an OfferedCapability already links to this skill via sda:realisedBy
            String functionClass = "Unknown";
            String capName = "Inferred-" + skillLocalName;
            boolean explicitExists = false;

            var capProp = model.createProperty(SDA_NS + "realisedBy");
            var capabilities = model.listSubjectsWithProperty(capProp, skill).toList();
            for (Resource cap : capabilities) {
                if (cap.hasProperty(org.apache.jena.vocabulary.RDF.type,
                    model.createResource(SDA_NS + "OfferedCapability"))) {
                    explicitExists = true;
                    var nameStmt = cap.getProperty(model.createProperty(SDA_NS + "capabilityName"));
                    if (nameStmt != null) {
                        functionClass = nameStmt.getString();
                        capName = functionClass;
                    }
                    break;
                }
            }

            result.add(new InferredCapability(
                capName, functionClass, engUnit, minRange, maxRange, skillLocalName));

            // If no explicit OfferedCapability links to this skill, add inferred triple
            if (!explicitExists) {
                String inferredUri = INST_NS + "InfCap-" + skillLocalName;
                Resource inferredCap = model.createResource(inferredUri);
                inferredCap.addProperty(org.apache.jena.vocabulary.RDF.type,
                    model.createResource(SDA_NS + "OfferedCapability"));
                inferredCap.addProperty(RDFS.label, "Inferred Capability for " + skillLocalName);
                inferredCap.addProperty(model.createProperty(SDA_NS + "capabilityName"), capName);
                inferredCap.addProperty(model.createProperty(SDA_NS + "realisedBy"), skill);
                log.debug("Added inferred OfferedCapability: {}", inferredUri);
            }
        }

        return result;
    }

    private static String getLocalName(Resource resource) {
        String uri = resource.getURI();
        if (uri == null) return resource.toString();
        int hash = uri.lastIndexOf('#');
        if (hash >= 0) return uri.substring(hash + 1);
        int slash = uri.lastIndexOf('/');
        if (slash >= 0) return uri.substring(slash + 1);
        return uri;
    }
}
