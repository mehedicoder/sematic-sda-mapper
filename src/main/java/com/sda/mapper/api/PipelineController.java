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
import java.util.Map;

/**
 * REST controller for executing the full end-to-end semantic mapping pipeline.
 * Accepts an input file (MTP AML, OPC UA Server Datasheet, or AAS JSON),
 * parses it, lifts triples into the ontology, runs OWL inference,
 * matches requirements via inline parameters, and generates
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
     * Requirements are specified by the user via inline parameters.
     *
     * @param file                the input file (MTP AML, OPC UA Server Datasheet, or AAS JSON)
     * @param protocol            the required communication protocol (e.g. "OPC-UA", "PROFINET")
     * @param accessMode          the required access mode (e.g. "read-only", "read-write")
     * @param minRecall           minimum recall threshold (default 0.90)
     * @param maxLatency          maximum latency threshold in ms (default 60)
     * @param onPlantNetwork      whether service must be on plant network (default true)
     * @param safetyIntegrityLevel optional SIL filter (e.g. "SIL-2")
     * @param determinismClass    optional determinism class filter (e.g. "HardRealTime")
     * @param networkZone         optional network zone filter (e.g. "OT-Zone-3")
     * @param persistToOntology   whether to persist parsed triples to ontology
     * @return the pipeline response with match result and manifests
     */
    @PostMapping("/execute")
    public ResponseEntity<?> execute(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "protocol", defaultValue = "OPC-UA") String protocol,
            @RequestParam(value = "accessMode", defaultValue = "read-only") String accessMode,
            @RequestParam(value = "minRecall", defaultValue = "0.90") double minRecall,
            @RequestParam(value = "maxLatency", defaultValue = "60") int maxLatency,
            @RequestParam(value = "onPlantNetwork", defaultValue = "true") boolean onPlantNetwork,
            @RequestParam(value = "safetyIntegrityLevel", required = false) String safetyIntegrityLevel,
            @RequestParam(value = "determinismClass", required = false) String determinismClass,
            @RequestParam(value = "networkZone", required = false) String networkZone,
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

        log.info("Pipeline execution requested: filename={}, size={}, protocol={}, accessMode={}",
            filename, file.getSize(), protocol, accessMode);

        Map<String, String> reqParams = new java.util.HashMap<>();
        reqParams.put("protocol", protocol);
        reqParams.put("accessMode", accessMode);
        reqParams.put("minRecall", String.valueOf(minRecall));
        reqParams.put("maxLatency", String.valueOf(maxLatency));
        reqParams.put("onPlantNetwork", String.valueOf(onPlantNetwork));
        if (safetyIntegrityLevel != null && !safetyIntegrityLevel.isBlank()) {
            reqParams.put("safetyIntegrityLevel", safetyIntegrityLevel);
        }
        if (determinismClass != null && !determinismClass.isBlank()) {
            reqParams.put("determinismClass", determinismClass);
        }
        if (networkZone != null && !networkZone.isBlank()) {
            reqParams.put("networkZone", networkZone);
        }

        try {
            PipelineResult result = pipelineService.execute(filename, file.getInputStream(), reqParams,
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
