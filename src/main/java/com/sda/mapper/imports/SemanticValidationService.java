package com.sda.mapper.imports;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validates the completeness of semantic information per asset after parsing.
 * Operates on the merged Jena Model to detect gaps in the CSS-SDA hierarchy
 * before the ontology merge is committed.
 */
@Component
public class SemanticValidationService {

    private static final Logger log = LoggerFactory.getLogger(SemanticValidationService.class);

    private final MissingInformationDetector detector;

    public SemanticValidationService(MissingInformationDetector detector) {
        this.detector = detector;
    }

    /**
     * Validates semantic completeness of the merged model.
     *
     * @param mergedModel the model containing newly parsed entities
     * @return validation result with missing items and overall status
     */
    public SemanticValidationResult validate(Model mergedModel) {
        List<MissingItem> missingItems = detector.detectMissing(mergedModel);

        boolean hasRequiredMissing = missingItems.stream()
            .anyMatch(item -> item.category() == MissingCategory.MISSING_COMMUNICATION_BINDING
                || item.category() == MissingCategory.MISSING_DEPLOYMENT_CONTEXT
                || item.category() == MissingCategory.MISSING_CAPABILITY);

        boolean hasWarnings = missingItems.stream()
            .anyMatch(item -> item.category() == MissingCategory.MISSING_SEMANTIC_ID
                || item.category() == MissingCategory.MISSING_ENGINEERING_UNIT
                || item.category() == MissingCategory.MISSING_DEVICE_METADATA);

        String status;
        if (hasRequiredMissing) {
            status = "BLOCKED";
        } else if (hasWarnings) {
            status = "WARNING";
        } else {
            status = "COMPLETE";
        }

        log.info("Semantic validation: status={}, {} missing items ({} required, {} warnings)",
            status, missingItems.size(),
            missingItems.stream().filter(item -> !item.resolvable()).count(),
            missingItems.size() - missingItems.stream().filter(item -> !item.resolvable()).count());

        return new SemanticValidationResult(status, missingItems);
    }

    /**
     * Result of semantic validation.
     */
    public record SemanticValidationResult(
        String status,
        List<MissingItem> missingItems
    ) {
        public boolean isBlocked() {
            return "BLOCKED".equals(status);
        }

        public boolean hasWarnings() {
            return "WARNING".equals(status);
        }
    }
}
