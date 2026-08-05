package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.UploadedFileInfo;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Session-scoped state of the mapping hub: the accumulated canonical model,
 * the uploaded files and formats, and the latest inferred entities. Uploads
 * enrich the model instead of replacing it, supporting iterative mapping.
 */
@VaadinSessionScope
@Component
public class MappingHubSession {

    private final UUID id = UUID.randomUUID();
    private final CanonicalAssetModel model = new CanonicalAssetModel();
    private final List<UploadedFileInfo> files = new ArrayList<>();
    private final Set<AssetFileFormat> uploadedFormats = new LinkedHashSet<>();
    private List<CanonicalEntity> inferred = List.of();

    /**
     * Merges a freshly parsed model and registers the uploaded file.
     *
     * @param info   description of the uploaded file
     * @param parsed the parsed canonical model
     */
    public void ingest(UploadedFileInfo info, CanonicalAssetModel parsed) {
        model.merge(parsed);
        files.add(info);
        uploadedFormats.add(info.format());
    }

    /**
     * Adds a manually entered entity to the model.
     *
     * @param entity the manually entered entity
     */
    public void addManualEntity(CanonicalEntity entity) {
        model.addEntity(entity);
    }

    /**
     * Replaces the inferred entities (recomputed on every pipeline run).
     *
     * @param entities the inferred entities
     */
    public void setInferred(List<CanonicalEntity> entities) {
        this.inferred = List.copyOf(entities);
    }

    /**
     * Clears all session state.
     */
    public void reset() {
        model.clear();
        files.clear();
        uploadedFormats.clear();
        inferred = List.of();
    }

    /**
     * Returns the accumulated canonical model.
     *
     * @return the canonical model
     */
    public CanonicalAssetModel getModel() {
        return model;
    }

    /**
     * Returns the durable session identifier shared with database storage.
     *
     * @return the session id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the latest inferred entities.
     *
     * @return an immutable list of inferred entities
     */
    public List<CanonicalEntity> getInferred() {
        return inferred;
    }

    /**
     * Returns the uploaded files in upload order.
     *
     * @return an immutable list of uploaded files
     */
    public List<UploadedFileInfo> getFiles() {
        return Collections.unmodifiableList(files);
    }

    /**
     * Returns the formats that have already been uploaded.
     *
     * @return an immutable set of uploaded formats
     */
    public Set<AssetFileFormat> getUploadedFormats() {
        return Collections.unmodifiableSet(uploadedFormats);
    }
}
