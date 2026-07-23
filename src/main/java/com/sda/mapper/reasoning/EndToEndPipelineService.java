package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.ontology.ServiceRegistrationService;
import com.sda.mapper.parser.ParseResult;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * End-to-end pipeline that chains Parse → Lift → Infer → Match.
 * Accepts an input file, parses it into a Jena Model, lifts (merges) it
 * into the base ontology model, runs inference, and matches requirements
 * against the enriched model.
 * <p>
 * Optionally creates a new {@code sda:Requirement} in the model from
 * form-provided parameters before matching.
 */
@Service
public class EndToEndPipelineService {

    private static final Logger log = LoggerFactory.getLogger(EndToEndPipelineService.class);

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private final com.sda.mapper.parser.UploadService uploadService;
    private final OntologyModelLoader ontologyModelLoader;
    private final OntologyInferenceService inferenceService;
    private final SemanticReasonerService reasonerService;
    private final RequirementDiscoveryService requirementDiscoveryService;
    private final ServiceRegistrationService serviceRegistrationService;

    public EndToEndPipelineService(
            com.sda.mapper.parser.UploadService uploadService,
            OntologyModelLoader ontologyModelLoader,
            OntologyInferenceService inferenceService,
            SemanticReasonerService reasonerService,
            RequirementDiscoveryService requirementDiscoveryService,
            ServiceRegistrationService serviceRegistrationService) {
        this.uploadService = uploadService;
        this.ontologyModelLoader = ontologyModelLoader;
        this.inferenceService = inferenceService;
        this.reasonerService = reasonerService;
        this.requirementDiscoveryService = requirementDiscoveryService;
        this.serviceRegistrationService = serviceRegistrationService;
    }

    /**
     * Executes the full pipeline: parse → lift → infer → match.
     * Optionally creates a new requirement from form parameters before matching.
     *
     * @param filename            the original filename
     * @param input               the file content stream
     * @param requirementIds      optional list of requirement IDs to match (null or empty = discover all)
     * @param newRequirementParams optional form parameters for creating a new requirement (null = skip)
     * @param persistToOntology   if true, persist parsed triples to the uploaded instances TTL file
     * @return a {@link PipelineResult} containing parse info, match results, and warnings
     */
    public PipelineResult execute(String filename, InputStream input,
                                   List<String> requirementIds,
                                   Map<String, String> newRequirementParams,
                                   boolean persistToOntology) {
        log.info("Starting end-to-end pipeline for: {}", filename);
        long startTime = System.currentTimeMillis();

        // 1. Parse
        ParseResult parseResult = uploadService.parseUpload(filename, input);
        log.info("Parse completed: {} triples, type={}", parseResult.tripleCount(), parseResult.fileType());

        // 2. Lift — merge parsed model into base ontology model
        List<String> warnings = new ArrayList<>();
        Model parsedModel = parseResult.model();
        if (parsedModel == null) {
            warnings.add("Parser did not produce a Jena Model — skipping lift step");
        } else {
            Model baseModel = ontologyModelLoader.getModel();
            baseModel.add(parsedModel);
            log.info("Lift completed: merged {} triples into base model (now {} triples)",
                parsedModel.size(), baseModel.size());
        }

        // 2.5 Create new requirement from form params if provided
        List<String> targetRequirements = new ArrayList<>(
            (requirementIds == null || requirementIds.isEmpty())
                ? requirementDiscoveryService.discoverRequirementIds()
                : requirementIds);

        if (newRequirementParams != null && !newRequirementParams.isEmpty()) {
            String newReqId = createRequirementInModel(newRequirementParams);
            if (newReqId != null) {
                targetRequirements.add(newReqId);
                warnings.add("Created new requirement: " + newReqId);
                log.info("Created new requirement {} with params: {}", newReqId, newRequirementParams);
            }
        }

        // 3. Infer — rerun inference on the enriched model
        inferenceService.runInference();
        int inferredTriples = inferenceService.getInferredTripleCount();
        log.info("Inference completed: {} inferred triples", inferredTriples);

        // 4. Match requirements
        Map<String, ServiceMatchResult> matches = new LinkedHashMap<>();
        for (String reqId : targetRequirements) {
            ServiceMatchResult result = reasonerService.matchByRequirementId(reqId);
            matches.put(reqId, result);
            log.info("Match {}: {} — {}", reqId, result.matched() ? result.serviceName() : "no match",
                result.explanation());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Pipeline completed in {}ms", elapsed);

        // 5. Persist to ontology if requested
        boolean persisted = false;
        if (persistToOntology && parsedModel != null) {
            try {
                serviceRegistrationService.registerService(parsedModel);
                persisted = true;
                warnings.add("Persisted " + parsedModel.size() + " triples to ontology");
                log.info("Persisted {} triples to uploaded instances", parsedModel.size());
            } catch (com.sda.mapper.error.OntologyConflictException e) {
                warnings.add("Persistence blocked: " + e.getMessage());
                log.warn("Persistence blocked by conflict: {}", e.getMessage());
            }
        }

        return new PipelineResult(
            parseResult.fileType(),
            parseResult.tripleCount(),
            parsedModel != null ? (int) parsedModel.size() : 0,
            inferredTriples,
            matches,
            warnings,
            elapsed,
            persisted
        );
    }

    /**
     * Creates a new {@code sda:Requirement} resource in the ontology model
     * from the provided form parameters.
     *
     * @param params form parameters (title, description, protocol, accessMode, etc.)
     * @return the generated requirement ID (e.g. "REQ-a1b2c3d4"), or null if creation failed
     */
    private String createRequirementInModel(Map<String, String> params) {
        try {
            String requirementId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Model model = ontologyModelLoader.getModel();
            String resourceUri = INST_NS + requirementId;

            Resource requirement = model.createResource(resourceUri);
            requirement.addProperty(
                model.createProperty(RDF_TYPE),
                model.createResource(SDA_NS + "Requirement"));

            requirement.addProperty(model.createProperty(SDA_NS + "requirementID"), requirementId);

            putIfPresent(model, requirement, params, "title");
            putIfPresent(model, requirement, params, "description");
            putIfPresent(model, requirement, params, "protocol");
            putIfPresent(model, requirement, params, "accessMode");
            putIfPresent(model, requirement, params, "networkZone");
            putIfPresent(model, requirement, params, "determinismClass");
            putIfPresent(model, requirement, params, "safetyIntegrityLevel");

            // Create constraints for recall and latency
            if (params.containsKey("minRecall")) {
                String constraintUri = INST_NS + requirementId + "-Constraint-MinRecall";
                Resource constraint = model.createResource(constraintUri);
                constraint.addProperty(
                    model.createProperty(RDF_TYPE),
                    model.createResource(SDA_NS + "RequirementConstraint"));
                constraint.addProperty(model.createProperty(SDA_NS + "constraintType"), "recall");
                constraint.addProperty(model.createProperty(SDA_NS + "constraintValue"), params.get("minRecall"));
                constraint.addProperty(model.createProperty(SDA_NS + "constraintOperator"), ">=");
                requirement.addProperty(model.createProperty(SDA_NS + "hasConstraint"), constraint);
            }
            if (params.containsKey("maxLatency")) {
                String constraintUri = INST_NS + requirementId + "-Constraint-MaxLatency";
                Resource constraint = model.createResource(constraintUri);
                constraint.addProperty(
                    model.createProperty(RDF_TYPE),
                    model.createResource(SDA_NS + "RequirementConstraint"));
                constraint.addProperty(model.createProperty(SDA_NS + "constraintType"), "latency");
                constraint.addProperty(model.createProperty(SDA_NS + "constraintValue"), params.get("maxLatency"));
                constraint.addProperty(model.createProperty(SDA_NS + "constraintOperator"), "<=");
                requirement.addProperty(model.createProperty(SDA_NS + "hasConstraint"), constraint);
            }

            return requirementId;
        } catch (Exception e) {
            log.error("Failed to create requirement from params: {}", params, e);
            return null;
        }
    }

    private void putIfPresent(Model model, Resource resource,
                               Map<String, String> params, String key) {
        String value = params.get(key);
        if (value != null && !value.isBlank()) {
            resource.addProperty(model.createProperty(SDA_NS + key), value);
        }
    }

    /**
     * Result of the full end-to-end pipeline.
     */
    public record PipelineResult(
        String fileType,
        int parsedTripleCount,
        int liftedTripleCount,
        int inferredTripleCount,
        Map<String, ServiceMatchResult> matches,
        List<String> warnings,
        long elapsedMs,
        boolean persisted
    ) {
    }
}
