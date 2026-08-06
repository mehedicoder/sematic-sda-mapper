package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Java equivalent of the SWRL R-Verify rule from the paper (Table I).
 * <p>
 * After R-Deploy marks a UseCaseRequirement as "Matched", this service evaluates
 * each {@code sda:VerificationCriterion} linked via {@code sda:verifiedBy}
 * against the matched Service's actual metrics. If all criteria are satisfied,
 * lifecycleStatus is advanced to "Verified".
 * <p>
 * Paper reference: R-Verify SWRL rule — "Updates lifecycleStatus to Verified
 * once all verificationCriterion are satisfied".
 */
@Service
public class RVerifyService {

    private static final Logger log = LoggerFactory.getLogger(RVerifyService.class);

    private static final String INST_NS = "http://sda-platform.org/instance#";
    private static final String SDA_NS = "http://sda-platform.org/ontology#";

    private final OntologyModelLoader ontologyModelLoader;

    public RVerifyService(OntologyModelLoader ontologyModelLoader) {
        this.ontologyModelLoader = ontologyModelLoader;
    }

    /**
     * Evaluates verification criteria for a matched requirement.
     * If all criteria pass, lifecycleStatus transitions to "Verified".
     *
     * @param requirementId the requirement ID (e.g. "REQ-001")
     * @return verification result with pass/fail details
     */
    public VerifyResult execute(String requirementId) {
        Model model = ontologyModelLoader.getModel();
        Resource reqResource = model.createResource(INST_NS + requirementId);

        Property verifiedByProp = model.createProperty(SDA_NS + "verifiedBy");
        Property matchedCandidateProp = model.createProperty(SDA_NS + "matchedCandidate");

        if (!reqResource.hasProperty(matchedCandidateProp)) {
            log.debug("R-Verify: {} has no matchedCandidate — skipping", requirementId);
            return new VerifyResult(false, "Not matched yet", List.of());
        }

        Resource serviceResource = reqResource.getPropertyResourceValue(matchedCandidateProp);
        if (serviceResource == null) {
            return new VerifyResult(false, "No matched service found", List.of());
        }

        var verifiedByStmts = reqResource.listProperties(verifiedByProp);
        List<CriterionResult> criteria = new ArrayList<>();
        boolean allPassed = true;

        while (verifiedByStmts.hasNext()) {
            Statement stmt = verifiedByStmts.next();
            Resource criterion = stmt.getObject().asResource();

            CriterionResult result = evaluateCriterion(model, criterion, serviceResource);
            criteria.add(result);
            if (!result.passed()) {
                allPassed = false;
            }
        }

        if (criteria.isEmpty()) {
            log.debug("R-Verify: {} has no VerificationCriterion — skipping", requirementId);
            return new VerifyResult(false, "No verification criteria defined", List.of());
        }

        Property lifecycleStatusProp = model.createProperty(SDA_NS + "lifecycleStatus");

        if (allPassed) {
            reqResource.removeAll(lifecycleStatusProp);
            reqResource.addProperty(lifecycleStatusProp,
                ResourceFactory.createStringLiteral("Verified"));
            log.info("R-Verify: lifecycleStatus({}) = Verified", requirementId);
            return new VerifyResult(true, "All criteria satisfied", criteria);
        } else {
            log.info("R-Verify: {} not all criteria satisfied — stays Matched", requirementId);
            return new VerifyResult(false, "Some criteria not satisfied", criteria);
        }
    }

    private CriterionResult evaluateCriterion(Model model, Resource criterion,
                                               Resource serviceResource) {
        Property testMethodProp = model.createProperty(SDA_NS + "testMethod");
        Property passThresholdProp = model.createProperty(SDA_NS + "passThreshold");

        String testMethod = getPropertyValue(criterion, testMethodProp);
        String thresholdStr = getPropertyValue(criterion, passThresholdProp);
        String label = criterion.hasProperty(org.apache.jena.vocabulary.RDFS.label)
            ? criterion.getProperty(org.apache.jena.vocabulary.RDFS.label).getString()
            : criterion.getLocalName();

        if (testMethod == null || thresholdStr == null) {
            return new CriterionResult(label, false, "Incomplete criterion definition");
        }

        Property metricProp = model.createProperty(SDA_NS + testMethod);
        Statement metricStmt = serviceResource.getProperty(metricProp);
        if (metricStmt == null || metricStmt.getLiteral() == null) {
            return new CriterionResult(label, false,
                "Metric '" + testMethod + "' not found on service");
        }

        double actualValue = metricStmt.getDouble();
        double threshold;
        try {
            threshold = Double.parseDouble(thresholdStr);
        } catch (NumberFormatException e) {
            return new CriterionResult(label, false, "Invalid threshold: " + thresholdStr);
        }

        // Latency-like metrics are upper-bounded, all other metrics are
        // lower-bounded.
        String operator = testMethod.toLowerCase().contains("latency")
            ? "<=" : ">=";

        boolean passed = operator.equals("<=")
            ? actualValue <= threshold : actualValue >= threshold;

        String detail = "%s: actual=%.2f %s %.2f → %s"
            .formatted(testMethod, actualValue, operator, threshold,
                passed ? "PASS" : "FAIL");

        return new CriterionResult(label, passed, detail);
    }

    private String getPropertyValue(Resource resource, Property property) {
        Statement stmt = resource.getProperty(property);
        if (stmt != null && stmt.getObject().isLiteral()) {
            return stmt.getString();
        }
        return null;
    }

    /**
     * Verification result containing pass/fail status and per-criterion details.
     */
    public record VerifyResult(boolean verified, String summary,
                                List<CriterionResult> criteria) {
    }

    /**
     * Result of evaluating a single VerificationCriterion.
     */
    public record CriterionResult(String name, boolean passed, String detail) {
    }
}
