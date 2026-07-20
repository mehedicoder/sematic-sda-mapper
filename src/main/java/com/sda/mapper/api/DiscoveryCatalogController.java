package com.sda.mapper.api;

import com.sda.mapper.opcua.OpcUaDiscoveryService;
import com.sda.mapper.opcua.OpcUaServerRegistry;
import com.sda.mapper.ui.catalog.DiscoveredServiceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for OPC UA service discovery.
 * Browses a live OPC UA server's address space and returns discovered variable nodes
 * as catalog entries.
 */
@RestController
@RequestMapping("/api/v1/catalog/discover")
public class DiscoveryCatalogController {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryCatalogController.class);

    private final OpcUaDiscoveryService discoveryService;
    private final OpcUaServerRegistry serverRegistry;

    public DiscoveryCatalogController(OpcUaDiscoveryService discoveryService,
                                       OpcUaServerRegistry serverRegistry) {
        this.discoveryService = discoveryService;
        this.serverRegistry = serverRegistry;
    }

    /**
     * Returns all known OPC UA servers from the registry.
     */
    @GetMapping("/servers")
    public ResponseEntity<List<OpcUaServerRegistry.ServerInfo>> getServers() {
        return ResponseEntity.ok(serverRegistry.getAllServers());
    }

    /**
     * Discovers services from a live OPC UA server.
     *
     * @param request the endpoint URL to discover from
     * @return discovered service entries with timing metadata
     */
    @PostMapping
    public ResponseEntity<?> discover(@RequestBody DiscoverRequest request) {
        if (request.endpointUrl() == null || request.endpointUrl().isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "endpointUrl is required")
            );
        }

        String endpointUrl = request.endpointUrl().trim();
        log.info("Discovery requested for: {}", endpointUrl);

        long start = System.currentTimeMillis();
        List<DiscoveredServiceEntry> entries = discoveryService.discover(endpointUrl);
        long elapsed = System.currentTimeMillis() - start;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("entries", entries);
        response.put("nodeCount", entries.size());
        response.put("discoveryTimeMs", elapsed);
        response.put("endpointUrl", endpointUrl);

        log.info("Discovery completed: {} nodes in {}ms from {}", entries.size(), elapsed, endpointUrl);
        return ResponseEntity.ok(response);
    }

    public record DiscoverRequest(String endpointUrl) {
    }
}
