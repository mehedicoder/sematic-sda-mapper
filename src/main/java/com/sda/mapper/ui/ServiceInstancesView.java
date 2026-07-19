package com.sda.mapper.ui;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.ArrayList;
import java.util.List;

/**
 * View showing registered CSS-SDA service instances from the ontology.
 */
@Route(value = "services", layout = MainLayout.class)
@PageTitle("Service Instances")
public class ServiceInstancesView extends VerticalLayout {

    private static final String SDA_NS = "http://www.example.org/sda#";

    public ServiceInstancesView(OntologyModelLoader ontologyModelLoader) {
        setSizeFull();
        setPadding(false);

        var header = new H2("Service Instances");
        header.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        if (!ontologyModelLoader.isLoaded()) {
            add(header, new Span("Ontology not loaded."));
            return;
        }

        Model model = ontologyModelLoader.getModel();
        List<ServiceEntry> services = queryServices(model);

        var count = new Span(services.size() + " service" + (services.size() == 1 ? "" : "s"));
        count.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        var grid = new Grid<>(ServiceEntry.class, false);
        grid.addColumn(ServiceEntry::name).setHeader("Name").setFlexGrow(1);
        grid.addColumn(ServiceEntry::protocol).setHeader("Protocol").setWidth("140px");
        grid.addColumn(ServiceEntry::accessMode).setHeader("Access Mode").setWidth("130px");
        grid.addColumn(e -> e.recall != null ? String.format("%.2f", e.recall) : "—")
            .setHeader("Recall").setWidth("90px");
        grid.addColumn(e -> e.latency != null ? e.latency + "ms" : "—")
            .setHeader("Latency").setWidth("90px");
        grid.addColumn(ServiceEntry::image).setHeader("Container Image").setFlexGrow(2);
        grid.setItems(services);
        grid.setWidthFull();
        grid.setHeightFull();

        add(header, count, grid);
        setFlexGrow(1, grid);
    }

    private static List<ServiceEntry> queryServices(Model model) {
        List<ServiceEntry> entries = new ArrayList<>();
        var services = model.listSubjectsWithProperty(
            RDF.type, model.createResource(SDA_NS + "Service"));

        while (services.hasNext()) {
            Resource svc = services.next();
            String name = getLabel(svc);
            String protocol = getProp(svc, "protocol");
            String accessMode = getProp(svc, "accessMode");
            Double recall = getDouble(svc, "reportedRecall");
            Integer latency = getInteger(svc, "reportedLatency");
            String image = getProp(svc, "image");
            entries.add(new ServiceEntry(name, protocol, accessMode, recall, latency, image));
        }
        return entries;
    }

    private static String getLabel(Resource r) {
        var stmt = r.getProperty(RDFS.label);
        return stmt != null ? stmt.getString() : r.getLocalName();
    }

    private static String getProp(Resource r, String prop) {
        var stmt = r.getProperty(r.getModel().createProperty(SDA_NS + prop));
        return stmt != null && stmt.getObject().isLiteral() ? stmt.getString() : null;
    }

    private static Double getDouble(Resource r, String prop) {
        var stmt = r.getProperty(r.getModel().createProperty(SDA_NS + prop));
        return stmt != null && stmt.getObject().isLiteral() ? stmt.getDouble() : null;
    }

    private static Integer getInteger(Resource r, String prop) {
        var stmt = r.getProperty(r.getModel().createProperty(SDA_NS + prop));
        return stmt != null && stmt.getObject().isLiteral() ? stmt.getInt() : null;
    }

    record ServiceEntry(String name, String protocol, String accessMode,
                         Double recall, Integer latency, String image) {
    }
}
