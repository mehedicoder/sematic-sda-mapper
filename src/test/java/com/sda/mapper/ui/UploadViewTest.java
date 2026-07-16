package com.sda.mapper.ui;

import com.sda.mapper.parser.UploadService;
import com.sda.mapper.parser.ParserFactory;
import com.sda.mapper.parser.MTPAmlParser;
import com.sda.mapper.parser.UaServerDatasheetParser;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Upload View UI")
class UploadViewTest {

    private UploadService createUploadService() {
        return new UploadService(new ParserFactory(new MTPAmlParser(), new UaServerDatasheetParser()));
    }

    @Test
    @DisplayName("Should be a VerticalLayout")
    void shouldBeVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(UploadView.class));
    }

    @Test
    @DisplayName("Should have Route annotation with upload path")
    void shouldHaveRouteAnnotation() {
        Route route = UploadView.class.getAnnotation(Route.class);
        assertNotNull(route, "UploadView must have @Route annotation");
        assertEquals("upload", route.value());
    }

    @Test
    @DisplayName("Should have correct @PageTitle")
    void shouldHaveCorrectPageTitle() {
        PageTitle pageTitle = UploadView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "UploadView must have @PageTitle annotation");
        assertEquals("Upload", pageTitle.value());
    }

    @Test
    @DisplayName("Should create UploadView with UploadService dependency")
    void shouldCreateViewWithService() {
        UploadView view = new UploadView(createUploadService());
        assertNotNull(view);
    }
}
