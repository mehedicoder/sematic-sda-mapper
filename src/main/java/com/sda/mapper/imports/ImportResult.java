package com.sda.mapper.imports;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores the aggregated result of a multi-file import session.
 * Contains counts of imported assets by type, conflicts, warnings, and completeness score.
 */
@Entity
@Table(name = "import_result")
public class ImportResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private ImportSession session;

    @Column(name = "assets_found", nullable = false)
    private int assetsFound = 0;

    @Column(name = "services_found", nullable = false)
    private int servicesFound = 0;

    @Column(name = "capabilities_found", nullable = false)
    private int capabilitiesFound = 0;

    @Column(name = "skills_found", nullable = false)
    private int skillsFound = 0;

    @Column(name = "communication_bindings_found", nullable = false)
    private int communicationBindingsFound = 0;

    @Column(name = "data_connectors_found", nullable = false)
    private int dataConnectorsFound = 0;

    @Column(name = "conflicts_detected", nullable = false)
    private int conflictsDetected = 0;

    @Column(nullable = false)
    private int warnings = 0;

    @Column(nullable = false)
    private int errors = 0;

    @Column(name = "completeness_score")
    private double completenessScore = 0.0;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    protected ImportResult() {
    }

    public ImportResult(ImportSession session) {
        this.session = session;
    }

    public UUID getId() {
        return id;
    }

    public ImportSession getSession() {
        return session;
    }

    public int getAssetsFound() {
        return assetsFound;
    }

    public void setAssetsFound(int assetsFound) {
        this.assetsFound = assetsFound;
    }

    public int getServicesFound() {
        return servicesFound;
    }

    public void setServicesFound(int servicesFound) {
        this.servicesFound = servicesFound;
    }

    public int getCapabilitiesFound() {
        return capabilitiesFound;
    }

    public void setCapabilitiesFound(int capabilitiesFound) {
        this.capabilitiesFound = capabilitiesFound;
    }

    public int getSkillsFound() {
        return skillsFound;
    }

    public void setSkillsFound(int skillsFound) {
        this.skillsFound = skillsFound;
    }

    public int getCommunicationBindingsFound() {
        return communicationBindingsFound;
    }

    public void setCommunicationBindingsFound(int communicationBindingsFound) {
        this.communicationBindingsFound = communicationBindingsFound;
    }

    public int getDataConnectorsFound() {
        return dataConnectorsFound;
    }

    public void setDataConnectorsFound(int dataConnectorsFound) {
        this.dataConnectorsFound = dataConnectorsFound;
    }

    public int getConflictsDetected() {
        return conflictsDetected;
    }

    public void setConflictsDetected(int conflictsDetected) {
        this.conflictsDetected = conflictsDetected;
    }

    public int getWarnings() {
        return warnings;
    }

    public void setWarnings(int warnings) {
        this.warnings = warnings;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public double getCompletenessScore() {
        return completenessScore;
    }

    public void setCompletenessScore(double completenessScore) {
        this.completenessScore = completenessScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
