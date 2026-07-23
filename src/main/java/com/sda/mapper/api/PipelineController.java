package com.sda.mapper.api;

import com.sda.mapper.api.dto.PipelineResponse;
import com.sda.mapper.api.dto.PipelineResponse.RequirementMatch;
import com.sda.mapper.api.dto.ValidationErrorResponse;
import com.sda.mapper.api.dto.ValidationErrorResponse.FieldError;
import com.sda.mapper.deployment.DockerComposeManifestService;
import com.sda.mapper.deployment.KubernetesManifestService;
import com.sda.mapper.error.OntologyLoadingException;
import com.sda.mapper.error.ParserException;
import com.sda.mapper.reasoning.EndToEndPipelineService;
import com.sda.mapper.reasoning.EndToEndPipelineService.PipelineResult;
import com.sda.mapper.reasoning.ServiceMatchResult;
import com.sda.mapper.reasoning.SparqlExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for executing the full end-to-end semantic mapping pipeline.
 * Accepts an input file (MTP AML, OPC UA Server Datasheet, or AAS JSON),
 * parses it, lifts triples into the ontology, runs OWL inference,
 * matches all predefined requirements via SPARQL, and generates
 * Docker Compose and Kubernetes deployment manifests.
 */
@RestController
@RequestMapping("/api/v1/pipeline")
public class PipelineController {

    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final EndToEndPipelineService pipelineService;
    private final DockerComposeManifestService dockerComposeService;
    private final KubernetesManifestService kubernetesService;

    public PipelineController(EndToEndPipelineService pipelineService,
                              DockerComposeManifestService dockerComposeService,
                              KubernetesManifestService kubernetesService) {
        this.pipelineService = pipelineService;
        this.dockerComposeService = dockerComposeService;
        this.kubernetesService = kubernetesService;
    }

    /**
     * Executes the full end-to-end pipeline: Parse → Lift → Infer → Match → Manifest.
     * When {@code requirementIds} is omitted or empty, all predefined requirements are matched.
     *
     * @param file           the input file (MTP AML, OPC UA Server Datasheet, or AAS JSON)
     * @param requirementIds optional comma-separated list of requirement IDs to match
     * @return the pipeline response with per-requirement match results and manifests
     */
    @PostMapping("/execute")
    public ResponseEntity<?> execute(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "requirementIds", required = false) String requirementIds,
            @RequestParam(value = "newReqTitle", required = false) String newReqTitle,
            @RequestParam(value = "newReqProtocol", required = false) String newReqProtocol,
            @RequestParam(value = "newReqAccessMode", required = false) String newReqAccessMode,
            @RequestParam(value = "newReqMinRecall", required = false) String newReqMinRecall,
            @RequestParam(value = "newReqMaxLatency", required = false) String newReqMaxLatency,
            @RequestParam(value = "newReqNetworkZone", required = false) String newReqNetworkZone,
            @RequestParam(value = "newReqDeterminism", required = false) String newReqDeterminism,
            @RequestParam(value = "newReqSil", required = false) String newReqSil,
            @RequestParam(value = "persistToOntology", required = false, defaultValue = "false") boolean persistToOntology) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                new ValidationErrorResponse(java.util.List.of(
                    new FieldError("file", "Uploaded file is empty")
                ))
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(
                new ValidationErrorResponse(java.util.List.of(
                    new FieldError("file", "File size exceeds maximum of 10 MB")
                ))
            );
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            return ResponseEntity.badRequest().body(
                new ValidationErrorResponse(java.util.List.of(
                    new FieldError("file", "File must have a non-empty filename")
                ))
            );
        }

        log.info("Pipeline execution requested: filename={}, size={}", filename, file.getSize());

        List<String> reqIds = null;
        if (requirementIds != null && !requirementIds.isBlank()) {
            reqIds = java.util.Arrays.stream(requirementIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
            log.info("Filtering pipeline to requirement IDs: {}", reqIds);
        }

        Map<String, String> newReqParams = null;
        if (newReqTitle != null || newReqProtocol != null) {
            newReqParams = new java.util.HashMap<>();
            if (newReqTitle != null) newReqParams.put("title", newReqTitle);
            if (newReqProtocol != null) newReqParams.put("protocol", newReqProtocol);
            if (newReqAccessMode != null) newReqParams.put("accessMode", newReqAccessMode);
            if (newReqMinRecall != null) newReqParams.put("minRecall", newReqMinRecall);
            if (newReqMaxLatency != null) newReqParams.put("maxLatency", newReqMaxLatency);
            if (newReqNetworkZone != null) newReqParams.put("networkZone", newReqNetworkZone);
            if (newReqDeterminism != null) newReqParams.put("determinismClass", newReqDeterminism);
            if (newReqSil != null) newReqParams.put("safetyIntegrityLevel", newReqSil);
            log.info("Creating new requirement from API params: {}", newReqParams);
        }

        try {
            PipelineResult result = pipelineService.execute(filename, file.getInputStream(), reqIds, newReqParams,
                persistToOntology);
            PipelineResponse response = toResponse(result);
            return ResponseEntity.ok(response);
        } catch (ParserException ex) {
            log.warn("Pipeline parse failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(
                new ValidationErrorResponse(java.util.List.of(
                    new FieldError("file", "Parse failed: " + ex.getMessage())
                ))
            );
        } catch (IOException ex) {
            log.error("Failed to read uploaded file", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ValidationErrorResponse(java.util.List.of(
                    new FieldError("file", "Failed to read uploaded file: " + ex.getMessage())
                ))
            );
        }
    }

    private PipelineResponse toResponse(PipelineResult result) {
        Map<String, RequirementMatch> matches = new LinkedHashMap<>();

        for (Map.Entry<String, ServiceMatchResult> entry : result.matches().entrySet()) {
            String reqId = entry.getKey();
            ServiceMatchResult matchResult = entry.getValue();

            if (matchResult.matched()) {
                String dockerComposeYaml = dockerComposeService.generate(matchResult);
                String kubernetesYaml = kubernetesService.generate(matchResult);
                matches.put(reqId, RequirementMatch.withManifests(matchResult, dockerComposeYaml, kubernetesYaml));
            } else {
                matches.put(reqId, RequirementMatch.noMatch(reqId, matchResult.explanation()));
            }
        }

        return new PipelineResponse(
            result.fileType(),
            result.parsedTripleCount(),
            result.liftedTripleCount(),
            result.inferredTripleCount(),
            result.elapsedMs(),
            matches,
            result.warnings()
        );
    }

    @ExceptionHandler(ParserException.class)
    public ResponseEntity<ValidationErrorResponse> handleParserError(ParserException ex) {
        return ResponseEntity.badRequest().body(
            new ValidationErrorResponse(java.util.List.of(
                new FieldError("parse", ex.getMessage())
            ))
        );
    }

    @ExceptionHandler(SparqlExecutionException.class)
    public ResponseEntity<ValidationErrorResponse> handleSparqlError(SparqlExecutionException ex) {
        return ResponseEntity.badRequest().body(
            new ValidationErrorResponse(java.util.List.of(
                new FieldError("sparql", ex.getMessage())
            ))
        );
    }

    @ExceptionHandler(OntologyLoadingException.class)
    public ResponseEntity<ValidationErrorResponse> handleOntologyError(OntologyLoadingException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            new ValidationErrorResponse(java.util.List.of(
                new FieldError("ontology", ex.getMessage())
            ))
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ValidationErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(
            new ValidationErrorResponse(java.util.List.of(
                new FieldError(ex.getParameterName(), ex.getMessage())
            ))
        );
    }
}
