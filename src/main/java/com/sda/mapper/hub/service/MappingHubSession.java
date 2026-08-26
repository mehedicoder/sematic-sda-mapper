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
    private final List<UploadedFile> uploads = new ArrayList<>();
    private final List<ActiveRequirement> activeRequirements = new ArrayList<>();
    private List<CanonicalEntity> inferred = List.of();
    private String userId;

    /**
     * A requirement the user has activated in the hub for catalogue matching.
     *
     * @param id            the requirement identifier (seeded {@code REQ-*} or a
     *                      user-supplied manual id)
     * @param description   the requirement description (manual entries only)
     * @param capabilityType the required capability type; {@code null} for a
     *                      pre-defined catalogue requirement resolved by id
     */
    public record ActiveRequirement(String id, String description, String capabilityType) {
    }

    /**
     * A previously uploaded file kept in the session together with its raw bytes
     * so an individual file can be removed and the canonical model rebuilt from
     * the remaining uploads (the model is a merged accumulation with no
     * per-entity provenance).
     *
     * @param fileName  original file name
     * @param format    detected format
     * @param sizeBytes file size in bytes
     * @param content   raw file bytes (needed to re-parse on removal)
     */
    public record UploadedFile(String fileName, AssetFileFormat format, long sizeBytes, byte[] content) {
        public UploadedFileInfo info() {
            return new UploadedFileInfo(fileName, format, sizeBytes);
        }
    }

    /**
     * Merges a freshly parsed model and registers the uploaded file (keeping its
     * raw bytes for later removal).
     *
     * @param info    description of the uploaded file
     * @param parsed  the parsed canonical model
     * @param content the raw uploaded bytes
     */
    public void ingest(UploadedFileInfo info, CanonicalAssetModel parsed, byte[] content) {
        model.merge(parsed);
        uploads.add(new UploadedFile(info.fileName(), info.format(), info.sizeBytes(), content));
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
        uploads.clear();
        activeRequirements.clear();
        inferred = List.of();
    }

    /**
     * Marks a catalogue requirement (e.g. a seeded {@code REQ-*}) as active so
     * the hub resolves it against the Service Catalogue during the MATCH step.
     *
     * @param requirementId the requirement identifier to activate
     */
    public void addActiveRequirement(String requirementId) {
        addActiveRequirement(requirementId, "", null);
    }

    /**
     * Marks a requirement (seeded or manually entered) as active.
     *
     * @param requirementId the requirement identifier to activate
     * @param description   the requirement description (manual entries)
     * @param capabilityType the required capability type (manual entries); or
     *                       {@code null} to resolve a seeded requirement by id
     */
    public void addActiveRequirement(String requirementId, String description,
                                     String capabilityType) {
        if (requirementId == null || requirementId.isBlank()) {
            return;
        }
        String id = requirementId.trim();
        activeRequirements.removeIf(a -> a.id().equals(id));
        activeRequirements.add(new ActiveRequirement(id,
            description == null ? "" : description.trim(), capabilityType));
    }

    /**
     * Deactivates a previously activated requirement.
     *
     * @param requirementId the requirement identifier to deactivate
     */
    public void removeActiveRequirement(String requirementId) {
        activeRequirements.removeIf(a -> a.id().equals(requirementId));
    }

    /**
     * Returns the requirements the user has activated for catalogue matching.
     *
     * @return an immutable view of the active requirements
     */
    public List<ActiveRequirement> getActiveRequirements() {
        return List.copyOf(activeRequirements);
    }

    /**
     * Binds the owning user name, captured lazily from the authenticated
     * principal on the first pipeline activity.
     *
     * @param userId authenticated user name, or {@code null} when unauthenticated
     */
    public void bindUser(String userId) {
        this.userId = userId;
    }

    /**
     * Returns the owning user name.
     *
     * @return the user name, or {@code null} for unassigned sessions
     */
    public String getUserId() {
        return userId;
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
        return uploads.stream().map(UploadedFile::info).toList();
    }

    /**
     * Returns the raw uploaded files (with content) for rebuilding the model.
     *
     * @return an immutable list of uploaded files
     */
    public List<UploadedFile> getUploads() {
        return List.copyOf(uploads);
    }

    /**
     * Removes the uploaded file at the given index and its held bytes.
     *
     * @param index position of the file to remove (0-based)
     */
    public void removeFileAt(int index) {
        uploads.remove(index);
    }

    /**
     * Returns the formats that have already been uploaded.
     *
     * @return an immutable set of uploaded formats
     */
    public Set<AssetFileFormat> getUploadedFormats() {
        return uploads.stream()
            .map(UploadedFile::format)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
