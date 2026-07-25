package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.ontology.ServiceRegistrationService;
import com.sda.mapper.parser.ParseResult;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * End-to-end pipeline that chains Parse → Lift → Infer → Match.
 * Accepts an input file, parses it into a Jena Model, lifts (merges) it
 * into the base ontology model, runs inference, and matches requirements
 * against the enriched model.
 */
@Service
public class EndToEndPipelineService {

    private static final Logger log = LoggerFactory.getLogger(EndToEndPipelineService.class);

    private final com.sda.mapper.parser.UploadService uploadService;
    private final OntologyModelLoader ontologyModelLoader;
    private final OntologyInferenceService inferenceService;
    private final SemanticReasonerService reasonerService;
    private final ServiceRegistrationService serviceRegistrationService;

    public EndToEndPipelineService(
            com.sda.mapper.parser.UploadService uploadService,
            OntologyModelLoader ontologyModelLoader,
            OntologyInferenceService inferenceService,
            SemanticReasonerService reasonerService,
            ServiceRegistrationService serviceRegistrationService) {
        this.uploadService = uploadService;
        this.ontologyModelLoader = ontologyModelLoader;
        this.inferenceService = inferenceService;
        this.reasonerService = reasonerService;
        this.serviceRegistrationService = serviceRegistrationService;
    }

    /**
     * Executes the full pipeline: parse → lift → infer → match.
     * Matches services against the provided requirement parameters using inline SPARQL.
     *
     * @param filename            the original filename
     * @param input               the file content stream
     * @param requirementParams   requirement parameters (protocol, accessMode, minRecall, maxLatency, etc.)
     * @param persistToOntology   if true, persist parsed triples to the uploaded instances TTL file
     * @return a {@link PipelineResult} containing parse info, match results, and warnings
     */
    public PipelineResult execute(String filename, InputStream input,
                                   Map<String, String> requirementParams,
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

        // 3. Infer — rerun inference on the enriched model
        inferenceService.runInference();
        int inferredTriples = inferenceService.getInferredTripleCount();
        log.info("Inference completed: {} inferred triples", inferredTriples);

        // 4. Match requirements via inline parameters
        Map<String, ServiceMatchResult> matches = new LinkedHashMap<>();
        String reqId = requirementParams.getOrDefault("requirementId", "REQ-USER");
        ServiceMatchResult result = reasonerService.matchInline(requirementParams);
        matches.put(reqId, result);
        log.info("Match {}: {} — {}", reqId, result.matched() ? result.serviceName() : "no match",
            result.explanation());

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
