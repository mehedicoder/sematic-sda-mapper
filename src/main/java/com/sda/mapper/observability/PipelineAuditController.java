package com.sda.mapper.observability;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST access to the historical pipeline audit timeline.
 *
 * <p>All filters are optional and combinable; results are ordered by
 * {@code started_at} ascending. Errors are returned as ProblemDetail (RFC 9457).</p>
 */
@RestController
@RequestMapping("/api/v1/pipeline-audits")
public class PipelineAuditController {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;

    private final PipelineAuditRepository repository;

    /**
     * Creates the controller.
     *
     * @param repository audit row persistence
     */
    public PipelineAuditController(PipelineAuditRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the chronological audit timeline for the given filters.
     *
     * @param sessionId     optional exact session id filter
     * @param requirementId optional exact requirement id filter
     * @param runId         optional exact run id filter
     * @param limit         optional row limit (default 200, max 1000)
     * @return the ordered timeline, possibly empty
     */
    @GetMapping
    public List<PipelineEvent> find(
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "requirementId", required = false) String requirementId,
            @RequestParam(value = "runId", required = false) String runId,
            @RequestParam(value = "limit", required = false) Integer limit) {

        int effectiveLimit = DEFAULT_LIMIT;
        if (limit != null) {
            if (limit < 1 || limit > MAX_LIMIT) {
                throw new IllegalArgumentException(
                    "limit must be between 1 and " + MAX_LIMIT + " but was " + limit);
            }
            effectiveLimit = limit;
        }
        return repository.find(sessionId, requirementId, runId, effectiveLimit);
    }

    /**
     * Maps invalid request parameters to a ProblemDetail response.
     *
     * @param ex the validation failure
     * @return 400 ProblemDetail
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleInvalidLimit(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid pipeline audit request");
        return ResponseEntity.badRequest().body(problem);
    }
}
