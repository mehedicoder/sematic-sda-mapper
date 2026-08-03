package com.sda.mapper.api;

import com.sda.mapper.api.dto.MatchRequirementRequest;
import com.sda.mapper.api.dto.MatchRequirementResponse;
import com.sda.mapper.api.dto.ValidationErrorResponse;
import com.sda.mapper.api.dto.ValidationErrorResponse.FieldError;
import com.sda.mapper.deployment.DockerComposeManifestService;
import com.sda.mapper.deployment.KubernetesManifestService;
import com.sda.mapper.error.OntologyLoadingException;
import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.RVerifyService;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.ServiceMatchResult;
import com.sda.mapper.reasoning.SparqlExecutionException;
import jakarta.validation.Valid;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sda.mapper.ontology.OntologyModelLoader;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for requirement matching and persistence.
 * Exposes endpoints for matching requirements against the CSS-SDA ontology
 * and for persisting new requirement definitions into the model.
 */
@RestController
@RequestMapping("/api/v1/requirements")
public class RequirementMatchingController {

    private static final Logger log = LoggerFactory.getLogger(RequirementMatchingController.class);

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "https://example.org/ontology/sda#";
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private final SemanticReasonerService reasonerService;
    private final OntologyModelLoader ontologyModelLoader;
    private final DockerComposeManifestService dockerComposeService;
    private final KubernetesManifestService kubernetesService;
    private final RDeployService rDeployService;
    private final RVerifyService rVerifyService;

    public RequirementMatchingController(SemanticReasonerService reasonerService,
                                         OntologyModelLoader ontologyModelLoader,
                                         DockerComposeManifestService dockerComposeService,
                                         KubernetesManifestService kubernetesService,
                                         RDeployService rDeployService,
                                         RVerifyService rVerifyService) {
        this.reasonerService = reasonerService;
        this.ontologyModelLoader = ontologyModelLoader;
        this.dockerComposeService = dockerComposeService;
        this.kubernetesService = kubernetesService;
        this.rDeployService = rDeployService;
        this.rVerifyService = rVerifyService;
    }

    /**
     * Matches a requirement against the ontology.
     * Mode A: supply {@code requirementId}.
     * Mode B: supply inline fields (protocol, accessMode, etc.).
     *
     * @param request the match request
     * @return the match response, or 400 if the request is invalid
     */
    @PostMapping("/match")
    public ResponseEntity<?> match(@Valid @RequestBody MatchRequirementRequest request) {
        boolean hasRequirementId = request.requirementId() != null
            && !request.requirementId().isBlank();
        boolean hasInlineFields = request.protocol() != null
            || request.accessMode() != null
            || request.functionClass() != null;

        if (!hasRequirementId && !hasInlineFields) {
            return ResponseEntity.badRequest().body(
                new ValidationErrorResponse(java.util.List.of(
                    new FieldError("request", "Provide either requirementId or inline fields (protocol, accessMode, etc.)")
                ))
            );
        }

        if (hasRequirementId && hasInlineFields) {
            return ResponseEntity.badRequest().body(
                new ValidationErrorResponse(java.util.List.of(
                    new FieldError("request", "Provide either requirementId or inline fields, not both")
                ))
            );
        }

        ServiceMatchResult result;
        if (hasRequirementId) {
            result = reasonerService.matchByRequirementId(request.requirementId());
        } else {
            Map<String, String> params = new HashMap<>();
            if (request.protocol() != null) params.put("protocol", request.protocol());
            if (request.accessMode() != null) params.put("accessMode", request.accessMode());
            if (request.functionClass() != null) params.put("functionClass", request.functionClass());
            if (request.safetyIntegrityLevel() != null) params.put("safetyIntegrityLevel", request.safetyIntegrityLevel());
            if (request.networkZone() != null) params.put("networkZone", request.networkZone());
            if (request.determinismClass() != null) params.put("determinismClass", request.determinismClass());
            if (request.engineeringUnit() != null) params.put("engineeringUnit", request.engineeringUnit());
            if (request.minRecall() != null) params.put("minRecall", String.valueOf(request.minRecall()));
            if (request.maxLatencyMs() != null) params.put("maxLatency", String.valueOf(request.maxLatencyMs()));
            result = reasonerService.matchInline(params);
        }

        if (result.matched()) {
            String dockerComposeYaml = dockerComposeService.generate(result);
            String kubernetesYaml = kubernetesService.generate(result);
            return ResponseEntity.ok(
                MatchRequirementResponse.withManifests(result, dockerComposeYaml, kubernetesYaml));
        }
        return ResponseEntity.ok(MatchRequirementResponse.fromServiceMatchResult(result));
    }

    @GetMapping("/{requirementId}/lifecycle")
    public ResponseEntity<Map<String, String>> getLifecycleStatus(
            @PathVariable String requirementId) {
        String status = rDeployService.getLifecycleStatus(requirementId);
        return ResponseEntity.ok(Map.of(
            "requirementId", requirementId,
            "lifecycleStatus", status,
            "matched", String.valueOf(rDeployService.isMatched(requirementId))
        ));
    }

    @PostMapping("/{requirementId}/verify")
    public ResponseEntity<Map<String, Object>> verifyRequirement(
            @PathVariable String requirementId) {
        RVerifyService.VerifyResult result = rVerifyService.execute(requirementId);
        return ResponseEntity.ok(Map.of(
            "requirementId", requirementId,
            "verified", result.verified(),
            "summary", result.summary(),
            "criteria", result.criteria().stream()
                .map(cr -> Map.of(
                    "name", cr.name(),
                    "passed", String.valueOf(cr.passed()),
                    "detail", cr.detail()
                ))
                .toList()
        ));
    }

    /**
     * Persists a new requirement into the ontology model as RDF triples.
     * The {@code requirementId} is mandatory — the system never auto-generates
     * requirement IDs; requirements must be defined by the user.
     *
     * @param request the requirement fields
     * @return 201 with the created requirement ID, or 400 if requirementId is missing
     */
    @PostMapping
    public ResponseEntity<?> persistRequirement(
            @RequestBody MatchRequirementRequest request) {
        if (request.requirementId() == null || request.requirementId().isBlank()) {
            return ResponseEntity.badRequest().body(
                new ValidationErrorResponse(java.util.List.of(
                    new FieldError("requirementId",
                        "requirementId is required — the system does not auto-generate requirement IDs")
                ))
            );
        }
        String requirementId = request.requirementId();

        Model model = ontologyModelLoader.getModel();
        String resourceUri = INST_NS + requirementId;

        Resource requirement = model.createResource(resourceUri);
        requirement.addProperty(
            model.createProperty(RDF_TYPE),
            model.createResource(SDA_NS + "Requirement"));

        if (request.functionClass() != null) {
            requirement.addProperty(
                model.createProperty(SDA_NS + "functionClass"), request.functionClass());
        }
        if (request.protocol() != null) {
            requirement.addProperty(
                model.createProperty(SDA_NS + "protocol"), request.protocol());
        }
        if (request.accessMode() != null) {
            requirement.addProperty(
                model.createProperty(SDA_NS + "accessMode"), request.accessMode());
        }
        if (request.engineeringUnit() != null) {
            requirement.addProperty(
                model.createProperty(SDA_NS + "engineeringUnit"), request.engineeringUnit());
        }
        if (request.safetyIntegrityLevel() != null) {
            requirement.addProperty(
                model.createProperty(SDA_NS + "safetyIntegrityLevel"), request.safetyIntegrityLevel());
        }
        if (request.networkZone() != null) {
            requirement.addProperty(
                model.createProperty(SDA_NS + "networkZone"), request.networkZone());
        }
        if (request.determinismClass() != null) {
            requirement.addProperty(
                model.createProperty(SDA_NS + "determinismClass"), request.determinismClass());
        }
        if (request.nominalRangeMin() != null) {
            requirement.addProperty(
                model.createProperty(SDA_NS + "nominalRangeMin"),
                String.valueOf(request.nominalRangeMin()));
        }
        if (request.nominalRangeMax() != null) {
            requirement.addProperty(
                model.createProperty(SDA_NS + "nominalRangeMax"),
                String.valueOf(request.nominalRangeMax()));
        }

        log.info("Persisted requirement {} to ontology model", requirementId);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("requirementId", requirementId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();
        return ResponseEntity.badRequest().body(new ValidationErrorResponse(fieldErrors));
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
}
