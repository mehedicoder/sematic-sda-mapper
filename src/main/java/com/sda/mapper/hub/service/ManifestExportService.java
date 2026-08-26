package com.sda.mapper.hub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sda.mapper.hub.error.MappingException;
import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.BlockingGap;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CapabilityMatch;
import com.sda.mapper.hub.model.CoverageEntry;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.model.Recommendation;
import com.sda.mapper.hub.model.UploadedFileInfo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MANIFEST stage: exports the complete mapping state as a JSON manifest with
 * metadata, uploaded files, per-class coverage, gaps, entities and
 * recommendations.
 */
@Service
public class ManifestExportService {

    private final MatchingService matchingService;
    private final RecommendationService recommendationService;
    private final DeploymentManifestGenerator deploymentGenerator;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates the manifest service with the stages it depends on.
     *
     * @param matchingService        the matching service
     * @param recommendationService  the recommendation service
     */
    public ManifestExportService(MatchingService matchingService,
                                 RecommendationService recommendationService) {
        this(matchingService, recommendationService, new DeploymentManifestGenerator());
    }

    /**
     * Creates the manifest service with an explicit deployment artifact generator.
     *
     * @param matchingService        the matching service
     * @param recommendationService  the recommendation service
     * @param deploymentGenerator    generates the Docker Compose / Kubernetes artifacts
     */
    public ManifestExportService(MatchingService matchingService,
                                 RecommendationService recommendationService,
                                 DeploymentManifestGenerator deploymentGenerator) {
        this.matchingService = matchingService;
        this.recommendationService = recommendationService;
        this.deploymentGenerator = deploymentGenerator;
    }

    /**
     * Builds the JSON manifest for the current mapping state.
     *
     * @param model       the canonical model
     * @param inferred    the inferred entities
     * @param files       the uploaded files
     * @param uploaded    the uploaded formats
     * @return the pretty-printed JSON manifest
     * @throws MappingException when the manifest cannot be serialized
     */
    public String build(CanonicalAssetModel model, List<CanonicalEntity> inferred,
                        List<UploadedFileInfo> files, Set<AssetFileFormat> uploaded) {
        return build(model, inferred, files, uploaded, List.of());
    }

    /**
     * Builds the JSON manifest, additionally embedding the reasoned
     * {@code deploymentManifest}: the R-Deploy runtime tuple committed for every
     * semantic capability match (required capability, offered capability, skill,
     * service, endpoint, protocol, network zone and SIL evidence). This is the
     * ontology-driven bridge from requirement to deployment (paper, R-Deploy).
     *
     * @param model       the canonical model
     * @param inferred    the inferred entities
     * @param files       the uploaded files
     * @param uploaded    the uploaded formats
     * @param matches     the semantic capability matches (R1-R3 output)
     * @return the pretty-printed JSON manifest
     * @throws MappingException when the manifest cannot be serialized
     */
    public String build(CanonicalAssetModel model, List<CanonicalEntity> inferred,
                        List<UploadedFileInfo> files, Set<AssetFileFormat> uploaded,
                        List<CapabilityMatch> matches) {
        GapReport report = matchingService.compute(model, inferred);
        List<Recommendation> recommendations = recommendationService.recommend(report, uploaded);
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("manifestVersion", "1.0");
            root.put("generatedAt", Instant.now().toString());
            root.put("coveragePercent", report.coveragePercent());
            root.putArray("files");
            for (UploadedFileInfo file : files) {
                root.withArray("files").add(file.describe());
            }

            ObjectNode coverage = root.putObject("coverage");
            for (CoverageEntry entry : report.entries()) {
                ObjectNode node = coverage.putObject(entry.className());
                node.put("layer", entry.layerId());
                node.put("status", entry.status().name());
                putStrings(node, "sources", entry.sources());
                putStrings(node, "missingFields", entry.missingFields());
            }

            ArrayNode layerCoverage = root.putArray("layerCoverage");
            for (com.sda.mapper.hub.model.LayerCoverage layer : report.layerCoverage()) {
                ObjectNode node = layerCoverage.addObject();
                node.put("layer", layer.layerId());
                node.put("total", layer.total());
                node.put("present", layer.present());
                node.put("inferred", layer.inferred());
                node.put("partial", layer.partial());
                node.put("missing", layer.missing());
                node.put("coveragePercent", layer.coveragePercent());
            }

            ArrayNode gaps = root.putArray("blockingGaps");
            for (BlockingGap gap : report.blockingGaps()) {
                ObjectNode node = gaps.addObject();
                node.put("className", gap.className());
                node.put("layer", gap.layerId());
                putStrings(node, "reasons", gap.reasons());
            }

            ObjectNode entities = root.putObject("entities");
            groupEntities(entities, model.allEntities());
            groupEntities(entities, inferred);

            ArrayNode recs = root.putArray("recommendations");
            for (Recommendation recommendation : recommendations) {
                ObjectNode node = recs.addObject();
                node.put("format", recommendation.format().label());
                node.put("reason", recommendation.reason());
                putStrings(node, "fills", recommendation.fills());
            }

            ArrayNode deploymentManifest = root.putArray("deploymentManifest");
            for (CapabilityMatch match : matches) {
                ObjectNode node = deploymentManifest.addObject();
                node.put("requiredCapabilityId", match.requiredCapabilityId());
                node.put("offeredCapabilityId", match.offeredCapabilityId());
                node.put("skillId", match.skillId());
                node.put("serviceName", match.serviceName());
                node.put("endpointUrl", match.endpointUrl());
                node.put("protocol", match.protocol());
                node.put("networkZone", match.networkZone());
                node.put("safetyIntegrityLevel", match.safetyIntegrityLevel());
                node.put("lifecycleStatus", "Matched");
                DeploymentManifestGenerator.DeploymentArtifacts artifacts =
                    deploymentGenerator.generate(match);
                node.put("dockerCompose", artifacts.dockerCompose());
                node.put("kubernetes", artifacts.kubernetes());
            }

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new MappingException("Failed to serialize mapping manifest", e);
        }
    }

    private void groupEntities(ObjectNode target, List<CanonicalEntity> entities) {
        for (CanonicalEntity entity : entities) {
            ArrayNode classArray = (ArrayNode) target.withArray(entity.className());
            ObjectNode node = classArray.addObject();
            node.put("id", entity.id());
            node.put("name", entity.name());
            entity.sourceFileName().ifPresent(source -> node.put("source", source));
            node.put("inferred", entity.inferred());
            for (Map.Entry<String, Object> attribute : entity.attributes().entrySet()) {
                node.set(attribute.getKey(), mapper.valueToTree(attribute.getValue()));
            }
        }
    }

    private static void putStrings(ObjectNode parent, String field, List<String> values) {
        ArrayNode array = parent.putArray(field);
        values.forEach(array::add);
    }
}
