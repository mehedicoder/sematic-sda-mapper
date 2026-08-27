package com.sda.mapper.ui.catalog;

import com.sda.mapper.hub.persistence.MappingHubPersistenceService;
import com.sda.mapper.hub.service.MappingHubSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads the offered-service catalogue for the Service Catalog view.
 *
 * <p>The catalogue is the durable {@code catalog_service} table populated by the
 * mapping hub pipeline (a projection of the base ontology unioned with every
 * uploaded instance triple). Reading from PostgreSQL instead of the live,
 * in-memory RDF model means all uploaded services are listed and the catalogue
 * survives application restarts.</p>
 */
@Service
public class ServiceCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ServiceCatalogService.class);

    private final MappingHubPersistenceService persistenceService;
    private final MappingHubSession session;

    public ServiceCatalogService(MappingHubPersistenceService persistenceService,
                                 MappingHubSession session) {
        this.persistenceService = persistenceService;
        this.session = session;
    }

    /**
     * Loads all offered services for the current session from the catalogue table.
     *
     * @return list of {@link ServiceCatalogEntry} for every registered service;
     *         returns empty list if no session is available (e.g. headless startup)
     */
    public List<ServiceCatalogEntry> loadCatalog() {
        if (session == null) {
            log.info("Service catalog loaded from store: 0 entries (no Vaadin session)");
            return List.of();
        }
        List<MappingHubPersistenceService.CatalogServiceRow> rows =
            persistenceService.loadCatalogEntries(session.getId());
        List<ServiceCatalogEntry> entries = rows.stream().map(this::toEntry).toList();
        log.info("Service catalog loaded from store: {} entries", entries.size());
        return entries;
    }

    private ServiceCatalogEntry toEntry(MappingHubPersistenceService.CatalogServiceRow row) {
        return new ServiceCatalogEntry(
            row.serviceName(),
            row.capabilityName(),
            row.skillName(),
            row.protocol(),
            row.accessMode(),
            row.securityMode(),
            row.recall(),
            row.latencyMs(),
            null, // containerImage — not part of the catalogue projection
            row.connectorName(),
            null, // informationModel — not part of the catalogue projection
            row.deploymentName(),
            row.safetyIntegrityLevel(),
            row.determinismClass(),
            row.networkZone());
    }
}
