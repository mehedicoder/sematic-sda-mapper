package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.observability.PipelineAuditPublisher;
import com.sda.mapper.observability.PipelineMetrics;
import com.sda.mapper.observability.PipelineStageObserver;
import com.sda.mapper.observability.PipelineTracingContext;
import com.sda.mapper.parser.ParseResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
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
 * Requirements are never auto-created or auto-selected. The pipeline only
 * matches the explicitly supplied requirement IDs and, optionally, a new
 * {@code sda:UseCaseRequirement} created from user-provided form parameters.
 */
@Service
public class EndToEndPipelineService {

    private static final Logger log = LoggerFactory.getLogger(EndToEndPipelineService.class);

    private static final String INST_NS = "http://sda-platform.org/instance#";
    private static final String SDA_NS = "http://sda-platform.org/ontology#";
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private final com.sda.mapper.parser.UploadService uploadService;
    private final OntologyModelLoader ontologyModelLoader;
    private final OntologyInferenceService inferenceService;
    private final SemanticReasonerService reasonerService;
    private final PipelineStageObserver stageObserver;
    private final PipelineTracingContext tracingContext;

    /**
     * Creates the pipeline service with no-op observability (unit tests).
     */
    public EndToEndPipelineService(
            com.sda.mapper.parser.UploadService uploadService,
            OntologyModelLoader ontologyModelLoader,
            OntologyInferenceService inferenceService,
            SemanticReasonerService reasonerService) {
        this(uploadService, ontologyModelLoader, inferenceService, reasonerService,
            new PipelineStageObserver(ObservationRegistry.NOOP,
                new PipelineAuditPublisher(null), new PipelineMetrics(new SimpleMeterRegistry())),
            new PipelineTracingContext(ObservationRegistry.NOOP));
    }

    /**
     * Creates the pipeline service with full pipeline observability.
     *
     * @param stageObserver  per-stage tracing/audit instrumentation
     * @param tracingContext run correlation context
     */
    @org.springframework.beans.factory.annotation.Autowired
    public EndToEndPipelineService(
            com.sda.mapper.parser.UploadService uploadService,
            OntologyModelLoader ontologyModelLoader,
            OntologyInferenceService inferenceService,
            SemanticReasonerService reasonerService,
            PipelineStageObserver stageObserver,
            PipelineTracingContext tracingContext) {
        this.uploadService = uploadService;
        this.ontologyModelLoader = ontologyModelLoader;
        this.inferenceService = inferenceService;
        this.reasonerService = reasonerService;
        this.stageObserver = stageObserver;
        this.tracingContext = tracingContext;
    }

    /**
     * Executes the full pipeline: parse → lift → infer → match.
     * Optionally creates a new requirement from form parameters before matching.
     * <p>
     * Only the explicitly supplied {@code requirementIds} are matched. A null or
     * empty list matches none — requirements are never auto-discovered. A new
     * requirement is created only from user-provided form parameters.
     *
     * @param filename            the original filename
     * @param input               the file content stream
     * @param requirementIds      list of requirement IDs to match (may be null/empty = no predefined matches)
     * @param newRequirementParams form parameters for creating a new requirement (null = skip)
     * @return a {@link PipelineResult} containing parse info, match results, and warnings
     */
    public PipelineResult execute(String filename, InputStream input,
                                   List<String> requirementIds,
                                   Map<String, String> newRequirementParams) {
        log.info("Starting end-to-end pipeline for: {}", filename);
        long startTime = System.currentTimeMillis();
        java.util.UUID runUuid = java.util.UUID.randomUUID();
        String runId = runUuid.toString();

        try (PipelineTracingContext.RunScope run =
                 tracingContext.openRun(runId, null, firstRequirement(requirementIds))) {

            // 1. Parse
            ParseResult parseResult;
            try (PipelineStageObserver.Stage stage = stageObserver.begin("PARSE", runUuid, null, null)) {
                parseResult = uploadService.parseUpload(filename, input);
                stage.detail("Parsed " + parseResult.tripleCount() + " triples (" + parseResult.fileType() + ")");
            }
            log.info("Parse completed: {} triples, type={}", parseResult.tripleCount(), parseResult.fileType());

            // 2. Lift — merge parsed model into base ontology model
            List<String> warnings = new ArrayList<>();
            Model parsedModel = parseResult.model();
            if (parsedModel == null) {
                warnings.add("Parser did not produce a Jena Model — skipping lift step");
            } else {
                try (PipelineStageObserver.Stage stage = stageObserver.begin("LIFT", runUuid, null, null)) {
                    Model baseModel = ontologyModelLoader.getModel();
                    baseModel.add(parsedModel);
                    stage.detail("Merged " + parsedModel.size() + " triples into base model");
                    log.info("Lift completed: merged {} triples into base model (now {} triples)",
                        parsedModel.size(), baseModel.size());
                }
            }

            // 2.5 Create new requirement from form params if provided
            List<String> targetRequirements = (requirementIds == null)
                ? new ArrayList<>()
                : new ArrayList<>(requirementIds);

            if (newRequirementParams != null && !newRequirementParams.isEmpty()) {
                String newReqId = createRequirementInModel(newRequirementParams);
                if (newReqId != null) {
                    targetRequirements.add(newReqId);
                    warnings.add("Created new requirement: " + newReqId);
                    log.info("Created new requirement {} with params: {}", newReqId, newRequirementParams);
                }
            }

            if (targetRequirements.isEmpty()) {
                warnings.add("No requirements selected — input was parsed and lifted but no requirements were matched");
                log.info("No requirements to match for: {}", filename);
            }

            // 3. Infer — rerun inference on the enriched model
            int inferredTriples;
            try (PipelineStageObserver.Stage stage = stageObserver.begin("INFER", runUuid, null, null)) {
                inferenceService.runInference();
                inferredTriples = inferenceService.getInferredTripleCount();
                stage.detail("Inferred " + inferredTriples + " triples");
            }
            log.info("Inference completed: {} inferred triples", inferredTriples);

            // 4. Match requirements — each match is instrumented inside
            //    SemanticReasonerService as a MATCH child span of this run.
            Map<String, ServiceMatchResult> matches = new LinkedHashMap<>();
            for (String reqId : targetRequirements) {
                ServiceMatchResult result = reasonerService.matchByRequirementId(reqId);
                matches.put(reqId, result);
                log.info("Match {}: {} — {}", reqId, result.matched() ? result.serviceName() : "no match",
                    result.explanation());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Pipeline completed in {}ms", elapsed);

            return new PipelineResult(
                parseResult.fileType(),
                parseResult.tripleCount(),
                parsedModel != null ? (int) parsedModel.size() : 0,
                inferredTriples,
                matches,
                warnings,
                elapsed
            );
        }
    }

    private String firstRequirement(List<String> requirementIds) {
        if (requirementIds != null && !requirementIds.isEmpty()) {
            return requirementIds.get(0);
        }
        return null;
    }

    /**
     * Creates a new {@code sda:UseCaseRequirement} resource in the ontology
     * model from the provided form parameters.
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
                model.createResource(SDA_NS + "UseCaseRequirement"));

            requirement.addProperty(model.createProperty(SDA_NS + "requirementID"), requirementId);

            putIfPresent(model, requirement, params, "title");
            putIfPresent(model, requirement, params, "description");
            putIfPresent(model, requirement, params, "protocol");
            putIfPresent(model, requirement, params, "accessMode");

            // Create constraints for recall and latency
            if (params.containsKey("minRecall")) {
                String constraintUri = INST_NS + requirementId + "-Constraint-MinRecall";
                Resource constraint = model.createResource(constraintUri);
                constraint.addProperty(
                    model.createProperty(RDF_TYPE),
                    model.createResource(SDA_NS + "UseCaseRequirementConstraint"));
                constraint.addProperty(model.createProperty(SDA_NS + "constraintType"), "recall");
                constraint.addProperty(model.createProperty(SDA_NS + "expression"),
                    "reportedRecall >= " + params.get("minRecall"));
                constraint.addProperty(model.createProperty(SDA_NS + "value"),
                    params.get("minRecall"));
                constraint.addProperty(model.createProperty(SDA_NS + "isHardConstraint"),
                    model.createTypedLiteral(true));
                requirement.addProperty(model.createProperty(SDA_NS + "hasConstraint"), constraint);
            }
            if (params.containsKey("maxLatency")) {
                String constraintUri = INST_NS + requirementId + "-Constraint-MaxLatency";
                Resource constraint = model.createResource(constraintUri);
                constraint.addProperty(
                    model.createProperty(RDF_TYPE),
                    model.createResource(SDA_NS + "UseCaseRequirementConstraint"));
                constraint.addProperty(model.createProperty(SDA_NS + "constraintType"), "latency");
                constraint.addProperty(model.createProperty(SDA_NS + "expression"),
                    "reportedLatency <= " + params.get("maxLatency"));
                constraint.addProperty(model.createProperty(SDA_NS + "value"),
                    params.get("maxLatency"));
                constraint.addProperty(model.createProperty(SDA_NS + "isHardConstraint"),
                    model.createTypedLiteral(true));
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
        long elapsedMs
    ) {
    }
}
