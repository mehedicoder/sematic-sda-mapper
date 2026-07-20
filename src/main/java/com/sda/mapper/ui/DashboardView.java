package com.sda.mapper.ui;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.OntologyInferenceService;
import com.sda.mapper.ui.component.DashboardCard;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;

/**
 * Dashboard view with animated status cards and metrics.
 * Shows counts for Registered Services, Capabilities, Skills,
 * Semantic Mappings, and Inferred Capabilities.
 */
@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashboard")
public class DashboardView extends VerticalLayout {

    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String CSS_NS = "http://www.example.org/css#";

    private final OntologyModelLoader ontologyModelLoader;
    private final OntologyInferenceService inferenceService;

    private DashboardCard servicesCard;
    private DashboardCard capabilitiesCard;
    private DashboardCard skillsCard;
    private DashboardCard mappingsCard;
    private DashboardCard inferredCard;

    public DashboardView(OntologyModelLoader ontologyModelLoader,
                         OntologyInferenceService inferenceService) {
        this.ontologyModelLoader = ontologyModelLoader;
        this.inferenceService = inferenceService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        var header = new H2("Dashboard");
        header.addClassNames(LumoUtility.Margin.Bottom.SMALL);
        add(header);

        if (!ontologyModelLoader.isLoaded()) {
            add(new Span("Ontology not loaded. Dashboard metrics unavailable."));
            return;
        }

        Model model = ontologyModelLoader.getModel();

        int serviceCount = countByType(model, SDA_NS + "Service");
        int capabilityCount = countByType(model, SDA_NS + "OfferedCapability")
            + countByType(model, SDA_NS + "RequiredCapability");
        int skillCount = countByType(model, CSS_NS + "Skill");
        int mappingCount = countByProperty(model, SDA_NS + "mapsToCapability");
        int inferredCount = inferenceService != null
            ? inferenceService.getInferredCapabilities().size() : 0;

        servicesCard = new DashboardCard(
            VaadinIcon.CONNECT, "Registered Services", serviceCount,
            "var(--lumo-primary-color)",
            createRouterLink(com.sda.mapper.ui.catalog.ServiceCatalogView.class, "Service Catalog"));

        capabilitiesCard = new DashboardCard(
            VaadinIcon.CUBE, "Capabilities", capabilityCount,
            "var(--lumo-success-color)",
            createRouterLink(com.sda.mapper.ui.catalog.ServiceCatalogView.class, "Service Catalog"));

        skillsCard = new DashboardCard(
            VaadinIcon.WRENCH, "Skills", skillCount,
            "var(--lumo-contrast-color)",
            createRouterLink(com.sda.mapper.ui.catalog.ServiceCatalogView.class, "Service Catalog"));

        mappingsCard = new DashboardCard(
            VaadinIcon.ARROW_RIGHT, "Semantic Mappings", mappingCount,
            "var(--lumo-warning-color)",
            createRouterLink(PipelineView.class, "Pipeline"));

        inferredCard = new DashboardCard(
            VaadinIcon.LIGHTBULB, "Inferred Capabilities", inferredCount,
            "var(--lumo-error-color)",
            createRouterLink(com.sda.mapper.ui.catalog.ServiceCatalogView.class, "Service Catalog"));

        var cardsLayout = new HorizontalLayout(
            servicesCard, capabilitiesCard, skillsCard, mappingsCard, inferredCard);
        cardsLayout.setWidthFull();
        cardsLayout.setSpacing(true);
        cardsLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

        add(cardsLayout);
        setFlexGrow(1, cardsLayout);
    }

    private static com.vaadin.flow.router.RouterLink createRouterLink(
            Class<? extends com.vaadin.flow.component.Component> viewClass, String label) {
        return new com.vaadin.flow.router.RouterLink(label, viewClass);
    }

    private static int countByType(Model model, String typeUri) {
        return model.listSubjectsWithProperty(
            RDF.type, model.createResource(typeUri)).toList().size();
    }

    private static int countByProperty(Model model, String propertyUri) {
        return model.listStatements(
            null, model.createProperty(propertyUri), (org.apache.jena.rdf.model.RDFNode) null
        ).toList().size();
    }
}
