package com.sda.mapper.imports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Suggests the most appropriate additional file type to resolve missing semantic information.
 * Maps missing categories to recommended file types based on what each file type provides.
 */
@Component
public class ImportRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(ImportRecommendationService.class);

    /**
     * Generates recommendations for resolving missing information.
     *
     * @param missingItems the detected missing items
     * @return map of missing category to recommended file types and actions
     */
    public Map<MissingCategory, Recommendation> recommend(List<MissingItem> missingItems) {
        Map<MissingCategory, Recommendation> recommendations = new LinkedHashMap<>();

        Map<MissingCategory, Long> countsByCategory = new HashMap<>();
        for (MissingItem item : missingItems) {
            countsByCategory.merge(item.category(), 1L, Long::sum);
        }

        for (var entry : countsByCategory.entrySet()) {
            MissingCategory category = entry.getKey();
            long count = entry.getValue();
            Recommendation rec = switch (category) {
                case MISSING_COMMUNICATION_BINDING -> new Recommendation(
                    List.of(FileType.OPC_UA_DATASHEET, FileType.AAS, FileType.OPC_UA_NODESET),
                    "Upload an OPC UA Server Datasheet or AAS submodel to provide endpoint and binding information",
                    (int) count
                );
                case MISSING_CAPABILITY -> new Recommendation(
                    List.of(FileType.MTP_AML),
                    "Upload a MTP AML file to provide service definitions and capability mappings",
                    (int) count
                );
                case MISSING_DEPLOYMENT_CONTEXT -> new Recommendation(
                    List.of(FileType.AAS, FileType.AASX, FileType.OPC_UA_DATASHEET),
                    "Upload an AAS submodel or OPC UA Datasheet to provide deployment context (SIL, zone, determinism)",
                    (int) count
                );
                case MISSING_SEMANTIC_ID -> new Recommendation(
                    List.of(FileType.AAS, FileType.AASX, FileType.MTP_AML),
                    "Upload an AAS submodel or MTP AML file to provide IEC CDD IRDI semantic identifiers",
                    (int) count
                );
                case MISSING_ENGINEERING_UNIT -> new Recommendation(
                    List.of(FileType.AAS, FileType.AASX, FileType.MTP_AML),
                    "Upload an AAS submodel or MTP AML file to provide UCUM engineering units",
                    (int) count
                );
                case MISSING_DEVICE_METADATA -> new Recommendation(
                    List.of(FileType.AAS, FileType.AASX, FileType.OPC_UA_DATASHEET),
                    "Upload an AAS submodel or OPC UA Datasheet to provide vendor, model, and version metadata",
                    (int) count
                );
                case MISSING_DIGITAL_TWIN -> new Recommendation(
                    List.of(FileType.AAS, FileType.AASX),
                    "Upload an AAS submodel to create digital twin associations",
                    (int) count
                );
                case DUPLICATE_ASSET -> new Recommendation(
                    List.of(),
                    "Resolve the duplicate by merging, replacing, or keeping both — no additional file needed",
                    (int) count
                );
            };
            recommendations.put(category, rec);
        }

        log.info("Generated {} recommendations for {} missing items across {} categories",
            recommendations.size(), missingItems.size(), countsByCategory.size());
        return recommendations;
    }

    /**
     * A recommendation for resolving a category of missing information.
     */
    public record Recommendation(
        List<FileType> suggestedFileTypes,
        String description,
        int affectedItemCount
    ) {
    }
}
