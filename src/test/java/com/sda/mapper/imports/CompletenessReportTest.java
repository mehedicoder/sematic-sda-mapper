package com.sda.mapper.imports;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Completeness Report — per-category scoring")
class CompletenessReportTest {

    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String CSS_NS = "http://www.example.org/css#";
    private static final String INST_NS = "http://www.example.org/instance#";

    private ImportReportService reportService;

    @BeforeEach
    void setUp() {
        OntologyModelLoader loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        reportService = new ImportReportService(
            new MissingInformationDetector(loader),
            new ImportRecommendationService()
        );
    }

    @Test
    @DisplayName("Should return 100% completeness for fully linked model")
    void shouldReturn100ForFullyLinkedModel() {
        Model model = ModelFactory.createDefaultModel();
        addFullyLinkedService(model);

        CompletenessReport report = reportService.generateCompletenessReport(model);
        assertEquals(100.0, report.overallScore(), 0.1);
        assertTrue(report.recommendedActions().isEmpty());
    }

    @Test
    @DisplayName("Should reduce score when CommunicationBinding is missing")
    void shouldReduceScoreForMissingBinding() {
        Model model = ModelFactory.createDefaultModel();
        Resource service = model.createResource(INST_NS + "Svc1");
        service.addProperty(RDF.type, model.createResource(CSS_NS + "Service"));
        service.addProperty(RDFS.label, "Svc1");

        CompletenessReport report = reportService.generateCompletenessReport(model);
        assertTrue(report.overallScore() < 100.0, "Score should be less than 100% when items are missing");
        assertFalse(report.missingItems().isEmpty());
        assertNotNull(report.categoryScores().get(MissingCategory.MISSING_COMMUNICATION_BINDING));
    }

    @Test
    @DisplayName("Should include per-category scores for all categories")
    void shouldIncludeAllCategoryScores() {
        Model model = ModelFactory.createDefaultModel();
        CompletenessReport report = reportService.generateCompletenessReport(model);

        for (MissingCategory category : MissingCategory.values()) {
            assertTrue(report.categoryScores().containsKey(category),
                "Should have score for category: " + category);
        }
    }

    @Test
    @DisplayName("Should count total entities analyzed")
    void shouldCountEntities() {
        Model model = ModelFactory.createDefaultModel();
        Resource r1 = model.createResource(INST_NS + "R1");
        r1.addProperty(RDF.type, model.createResource(CSS_NS + "Service"));
        Resource r2 = model.createResource(INST_NS + "R2");
        r2.addProperty(RDF.type, model.createResource(CSS_NS + "Capability"));

        CompletenessReport report = reportService.generateCompletenessReport(model);
        assertTrue(report.totalEntitiesAnalyzed() >= 2);
    }

    @Test
    @DisplayName("Should generate recommended actions for missing items")
    void shouldGenerateRecommendedActions() {
        Model model = ModelFactory.createDefaultModel();
        Resource service = model.createResource(INST_NS + "Svc1");
        service.addProperty(RDF.type, model.createResource(CSS_NS + "Service"));
        service.addProperty(RDFS.label, "Svc1");

        CompletenessReport report = reportService.generateCompletenessReport(model);
        assertFalse(report.recommendedActions().isEmpty(),
            "Should have recommended actions when items are missing");
    }

    private void addFullyLinkedService(Model model) {
        Resource service = model.createResource(INST_NS + "TestService");
        service.addProperty(RDF.type, model.createResource(CSS_NS + "Service"));
        service.addProperty(RDFS.label, "Test Service");

        Resource skill = model.createResource(INST_NS + "TestSkill");
        skill.addProperty(RDF.type, model.createResource(CSS_NS + "Skill"));
        skill.addProperty(model.createProperty(SDA_NS + "isImplementedBy"), service);

        Resource capability = model.createResource(INST_NS + "TestCapability");
        capability.addProperty(RDF.type, model.createResource(CSS_NS + "Capability"));
        capability.addProperty(model.createProperty(SDA_NS + "realisedBy"), skill);

        Resource binding = model.createResource(INST_NS + "TestBinding");
        binding.addProperty(RDF.type, model.createResource(CSS_NS + "CommunicationBinding"));
        service.addProperty(model.createProperty(SDA_NS + "usesBinding"), binding);

        Resource deploy = model.createResource(INST_NS + "TestDeploy");
        deploy.addProperty(RDF.type, model.createResource(CSS_NS + "DeploymentContext"));
        deploy.addProperty(model.createProperty(SDA_NS + "safetyIntegrityLevel"), "SIL-0");
        deploy.addProperty(model.createProperty(SDA_NS + "networkZone"), "OT-Zone-1");
        deploy.addProperty(model.createProperty(SDA_NS + "determinismClass"), "SoftRealTime");
        service.addProperty(model.createProperty(SDA_NS + "isDeployedBy"), deploy);
    }
}
