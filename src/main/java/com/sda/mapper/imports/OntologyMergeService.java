package com.sda.mapper.imports;

import com.sda.mapper.error.ImportException;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.ontology.OntologyPersistenceService;
import com.sda.mapper.parser.ParseResult;
import com.sda.mapper.parser.UploadService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Merges parsed files into a shared Apache Jena Model.
 * Each file is parsed independently, then results are merged by identifying
 * overlapping individuals via URI, label, and type.
 */
@Service
public class OntologyMergeService {

    private static final Logger log = LoggerFactory.getLogger(OntologyMergeService.class);

    private final UploadService uploadService;
    private final OntologyModelLoader ontologyModelLoader;
    private final OntologyPersistenceService ontologyPersistenceService;
    private final FileStorageService fileStorageService;

    public OntologyMergeService(UploadService uploadService, OntologyModelLoader ontologyModelLoader,
                                 OntologyPersistenceService ontologyPersistenceService,
                                 FileStorageService fileStorageService) {
        this.uploadService = uploadService;
        this.ontologyModelLoader = ontologyModelLoader;
        this.ontologyPersistenceService = ontologyPersistenceService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Parses and merges a list of uploaded files into a combined Jena Model.
     *
     * @param files the uploaded files to merge
     * @return merge result containing the merged model and statistics
     */
    public MergeResult mergeFiles(List<UploadedFile> files) {
        log.info("Merging {} files into ontology model", files.size());
        Model mergedModel = ModelFactory.createDefaultModel();
        int assets = 0, services = 0, capabilities = 0, skills = 0;
        int bindings = 0, connectors = 0;
        int warnings = 0;

        for (UploadedFile file : files) {
            try {
                ParseResult result = parseFile(file);
                if (result.model() != null) {
                    mergedModel.add(result.model());
                    var counts = countEntities(result.model());
                    assets += counts[0];
                    services += counts[1];
                    capabilities += counts[2];
                    skills += counts[3];
                    bindings += counts[4];
                    connectors += counts[5];
                }
            } catch (Exception e) {
                log.error("Failed to parse file '{}': {}", file.getFilename(), e.getMessage());
                warnings++;
            }
        }

        log.info("Merge complete: {} assets, {} services, {} capabilities, {} skills, {} bindings, {} connectors",
            assets, services, capabilities, skills, bindings, connectors);

        return new MergeResult(mergedModel, assets, services, capabilities, skills,
            bindings, connectors, warnings);
    }

    /**
     * Commits the merged model to the persistent ontology.
     * Each subject resource is tagged with its source filename for provenance tracking.
     */
    @Transactional
    public void commitToModel(UUID sessionId, List<UploadedFile> files) {
        log.info("Committing merged model for session {} to ontology", sessionId);
        Model mergedModel = ModelFactory.createDefaultModel();

        for (UploadedFile file : files) {
            try {
                ParseResult result = parseFile(file);
                if (result.model() != null) {
                    Model tagged = tagWithProvenance(result.model(), file.getFilename());
                    mergedModel.add(tagged);
                }
            } catch (Exception e) {
                log.error("Failed to parse file '{}' for commit: {}", file.getFilename(), e.getMessage());
            }
        }

        ontologyPersistenceService.saveUploadedInstances(mergedModel);
        ontologyModelLoader.reload();
        log.info("Committed {} triples from session {} to ontology", mergedModel.size(), sessionId);
    }

    /**
     * Tags each subject in the model with a source file provenance literal.
     */
    private Model tagWithProvenance(Model model, String filename) {
        var subjects = model.listSubjects().toList();
        for (Resource subject : subjects) {
            model.addLiteral(subject, RDFS.label,
                model.createLiteral("source:" + filename));
        }
        return model;
    }

    private ParseResult parseFile(UploadedFile file) {
        if (file.getStoragePath() == null || file.getStoragePath().isBlank()) {
            throw new ImportException("File has no stored content: " + file.getFilename());
        }
        try (InputStream is = fileStorageService.load(file.getStoragePath())) {
            return uploadService.parseUpload(file.getFilename(), is);
        } catch (Exception e) {
            throw new ImportException("Failed to parse file: " + file.getFilename(), e);
        }
    }

    private int[] countEntities(Model model) {
        int assets = 0, services = 0, capabilities = 0, skills = 0;
        int bindings = 0, connectors = 0;

        var iter = model.listStatements((Resource) null, RDF.type, (org.apache.jena.rdf.model.RDFNode) null);
        while (iter.hasNext()) {
            var stmt = iter.next();
            String typeUri = stmt.getObject().isResource()
                ? stmt.getObject().asResource().getURI() : "";
            if (typeUri == null) typeUri = "";

            if (typeUri.contains("FieldDevice") || typeUri.contains("MTPModule")
                || typeUri.contains("EnergyMeter") || typeUri.contains("BACnetDevice")) {
                assets++;
            } else if (typeUri.contains("Service")) {
                services++;
            } else if (typeUri.contains("Capability")) {
                capabilities++;
            } else if (typeUri.contains("Skill")) {
                skills++;
            } else if (typeUri.contains("CommunicationBinding")) {
                bindings++;
            } else if (typeUri.contains("DataConnector")) {
                connectors++;
            }
        }

        return new int[]{assets, services, capabilities, skills, bindings, connectors};
    }

    /**
     * Result of merging multiple files.
     */
    public record MergeResult(
        Model model,
        int assetsFound,
        int servicesFound,
        int capabilitiesFound,
        int skillsFound,
        int communicationBindingsFound,
        int dataConnectorsFound,
        int warnings
    ) {
    }
}
