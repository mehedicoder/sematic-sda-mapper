package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CoverageEntry;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.model.Recommendation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates context-aware recommendations for supplementary uploads.
 *
 * <p>Each format can fill a set of ontology classes directly and another set
 * through inference. Formats that have already been uploaded are never
 * re-recommended.</p>
 */
@Service
public class RecommendationService {

    private static final Map<AssetFileFormat, List<String>> DIRECT = Map.of(
        AssetFileFormat.AAS_XML, List.of("Asset", "UseCase", "Skill", "SkillParameter", "CommunicationBinding",
            "ProcessVariable", "Observation", "DataConnector", "SemanticConcept", "SafetyClass", "Service"),
        AssetFileFormat.AAS_JSON, List.of("Asset", "UseCase", "Skill", "SkillParameter", "CommunicationBinding",
            "ProcessVariable", "Observation", "DataConnector", "SemanticConcept", "SafetyClass", "Service"),
        AssetFileFormat.MTP_AML, List.of("Skill", "SkillParameter", "CommunicationBinding", "ProcessVariable", "DataConnector"),
        AssetFileFormat.OPCUA_JSON, List.of("Asset", "CommunicationBinding", "ProcessVariable", "SemanticConcept")
    );

    private static final Map<AssetFileFormat, List<String>> DERIVED = Map.of(
        AssetFileFormat.AAS_XML, List.of("Capability", "Sensor", "ServiceInstance", "ClusterNode", "NetworkZone", "DeploymentContext"),
        AssetFileFormat.AAS_JSON, List.of("Capability", "Sensor", "ServiceInstance", "ClusterNode", "NetworkZone", "DeploymentContext"),
        AssetFileFormat.MTP_AML, List.of("Capability", "Sensor", "ServiceInstance", "ClusterNode", "NetworkZone", "DeploymentContext"),
        AssetFileFormat.OPCUA_JSON, List.of("Sensor", "ServiceInstance", "ClusterNode", "NetworkZone", "DeploymentContext")
    );

    /**
     * Returns the formats that can contribute entities to the given class,
     * either directly or through inference.
     *
     * @param className the ontology class name
     * @return the formats that can fill this class
     */
    public List<AssetFileFormat> formatsThatCanFill(String className) {
        List<AssetFileFormat> result = new ArrayList<>();
        for (AssetFileFormat format : AssetFileFormat.values()) {
            if (DIRECT.getOrDefault(format, List.of()).contains(className)
                || DERIVED.getOrDefault(format, List.of()).contains(className)) {
                result.add(format);
            }
        }
        return result;
    }

    /**
     * Recommends upload formats that can fill currently open classes.
     *
     * @param report   the current coverage report
     * @param uploaded formats that have already been uploaded and must be excluded
     * @return recommendations sorted by how many open classes they fill
     */
    public List<Recommendation> recommend(GapReport report, Set<AssetFileFormat> uploaded) {
        List<Recommendation> result = new ArrayList<>();
        for (AssetFileFormat format : AssetFileFormat.values()) {
            if (uploaded.contains(format)) {
                continue;
            }
            List<String> fills = openClassesFillableBy(report, format);
            if (!fills.isEmpty()) {
                result.add(new Recommendation(format, reason(format, fills), fills));
            }
        }
        result.sort(Comparator.comparingInt((Recommendation r) -> r.fills().size()).reversed());
        return result;
    }

    private static List<String> openClassesFillableBy(GapReport report, AssetFileFormat format) {
        Set<String> fillable = new LinkedHashSet<>();
        fillable.addAll(DIRECT.getOrDefault(format, List.of()));
        fillable.addAll(DERIVED.getOrDefault(format, List.of()));
        List<String> open = new ArrayList<>();
        for (String className : fillable) {
            report.forClass(className).ifPresent(entry -> {
                if (entry.status() != CoverageStatus.PRESENT) {
                    open.add(className);
                }
            });
        }
        return open;
    }

    private static String reason(AssetFileFormat format, List<String> fills) {
        String preview = fills.size() > 5
            ? String.join(", ", fills.subList(0, 5)) + " \u2026"
            : String.join(", ", fills);
        return "An " + format.label() + " upload can fill " + fills.size()
            + " open class(es): " + preview;
    }
}
