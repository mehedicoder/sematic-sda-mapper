package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
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

    private static final String SDA_NS = "http://sda-platform.org/ontology#";

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
     * Returns capabilities derived from the CSS-SDA capability chain
     * ({@code sda:Capability} linked to {@code sda:Skill} via
     * {@code sda:realisedBy}).
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

        var capProp = model.createProperty(SDA_NS + "realisedBy");
        var capabilities = model.listSubjectsWithProperty(capProp).toList();

        for (Resource capability : capabilities) {
            if (!capability.hasProperty(org.apache.jena.vocabulary.RDF.type,
                model.createResource(SDA_NS + "OfferedCapability"))) {
                continue;
            }
            Resource skill = capability.getPropertyResourceValue(capProp);
            String skillLocalName = skill != null ? getLocalName(skill) : "Unknown";
            String capName = getLocalName(capability);
            var nameStmt = capability.getProperty(model.createProperty(SDA_NS + "capabilityName"));
            if (nameStmt != null) {
                capName = nameStmt.getString();
            }

            result.add(new InferredCapability(
                capName, capName, "", Double.NaN, Double.NaN, skillLocalName));
            log.debug("Inferred Capability: {} (realisedBy {})", capName, skillLocalName);
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
