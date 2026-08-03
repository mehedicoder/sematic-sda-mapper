package com.sda.mapper.monitoring;

import com.sda.mapper.ontology.OntologyModelLoader;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);
    private static final String SDA_NS = "https://example.org/ontology/sda#";

    private final OntologyModelLoader ontologyModelLoader;
    private final MeterRegistry meterRegistry;

    public MetricsService(OntologyModelLoader ontologyModelLoader, MeterRegistry meterRegistry) {
        this.ontologyModelLoader = ontologyModelLoader;
        this.meterRegistry = meterRegistry;
    }

    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        long memoryTotal = runtime.totalMemory();
        metrics.put("memoryUsed", memoryUsed);
        metrics.put("memoryTotal", memoryTotal);
        metrics.put("memoryUsedMb", Math.round(memoryUsed / (1024.0 * 1024.0)));
        metrics.put("memoryTotalMb", Math.round(memoryTotal / (1024.0 * 1024.0)));
        metrics.put("cpuUsage", getCpuUsage());

        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtimeBean.getUptime();
        metrics.put("uptime", formatUptime(uptimeMs));
        metrics.put("uptimeMs", uptimeMs);

        try {
            if (ontologyModelLoader.isLoaded()) {
                var model = ontologyModelLoader.getModel();
                metrics.put("ontologyTripleCount", model.size());

                long classCount = model.listSubjectsWithProperty(
                    RDF.type, RDFS.Class).toList().size();
                metrics.put("ontologyClassCount", classCount);

                long serviceCount = model.listSubjectsWithProperty(
                    RDF.type, model.createResource(SDA_NS + "Service")).toList().size();
                metrics.put("ontologyServiceCount", serviceCount);
            } else {
                metrics.put("ontologyTripleCount", 0);
                metrics.put("ontologyClassCount", 0);
                metrics.put("ontologyServiceCount", 0);
            }
        } catch (Exception e) {
            log.debug("Failed to read ontology metrics: {}", e.getMessage());
            metrics.put("ontologyTripleCount", 0);
            metrics.put("ontologyClassCount", 0);
            metrics.put("ontologyServiceCount", 0);
        }

        metrics.put("matchRequestCount", getCounterValue("sda.match.requests"));

        return metrics;
    }

    private double getCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean != null) {
            double load = osBean.getSystemLoadAverage();
            if (load >= 0) {
                return Math.min(load / osBean.getAvailableProcessors(), 1.0);
            }
        }
        return 0.0;
    }

    private long getCounterValue(String name) {
        try {
            var counter = meterRegistry.find(name).counter();
            return counter != null ? (long) counter.count() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatUptime(long uptimeMs) {
        long hours = TimeUnit.MILLISECONDS.toHours(uptimeMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMs) % 60;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}
