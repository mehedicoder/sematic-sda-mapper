package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
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
 * into the base ontology model, runs inference, and matches all three
 * predefined requirements against the enriched model.
 */
@Service
public class EndToEndPipelineService {

    private static final Logger log = LoggerFactory.getLogger(EndToEndPipelineService.class);

    private static final List<String> ALL_REQUIREMENTS = List.of("REQ-001", "REQ-002", "REQ-003");

    private final com.sda.mapper.parser.UploadService uploadService;
    private final OntologyModelLoader ontologyModelLoader;
    private final OntologyInferenceService inferenceService;
    private final SemanticReasonerService reasonerService;

    public EndToEndPipelineService(
            com.sda.mapper.parser.UploadService uploadService,
            OntologyModelLoader ontologyModelLoader,
            OntologyInferenceService inferenceService,
            SemanticReasonerService reasonerService) {
        this.uploadService = uploadService;
        this.ontologyModelLoader = ontologyModelLoader;
        this.inferenceService = inferenceService;
        this.reasonerService = reasonerService;
    }

    /**
     * Executes the full pipeline: parse → lift → infer → match.
     *
     * @param filename the original filename
     * @param input    the file content stream
     * @return a {@link PipelineResult} containing parse info, match results, and warnings
     */
    public PipelineResult execute(String filename, InputStream input) {
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

        // 4. Match all predefined requirements
        Map<String, ServiceMatchResult> matches = new LinkedHashMap<>();
        for (String reqId : ALL_REQUIREMENTS) {
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
