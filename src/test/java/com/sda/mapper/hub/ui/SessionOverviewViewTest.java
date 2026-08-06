package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.SessionOverview;
import com.sda.mapper.hub.model.UserSessionSummary;
import com.sda.mapper.hub.service.MappingHubService;
import com.sda.mapper.hub.service.SessionVisibilityService;
import com.sda.mapper.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Session Visibility View")
class SessionOverviewViewTest {

    private final SessionVisibilityService visibilityService = mock(SessionVisibilityService.class);
    private final MappingHubService hubService = mock(MappingHubService.class);

    private SessionOverviewView createView() {
        return new SessionOverviewView(visibilityService, hubService);
    }

    @Test
    @DisplayName("Should be a VerticalLayout")
    void shouldBeVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(SessionOverviewView.class));
    }

    @Test
    @DisplayName("Should have Route annotation with sessions path and MainLayout")
    void shouldHaveRouteAnnotation() {
        Route route = SessionOverviewView.class.getAnnotation(Route.class);
        assertNotNull(route, "SessionOverviewView must have @Route annotation");
        assertEquals("sessions", route.value());
        assertEquals(MainLayout.class, route.layout());
    }

    @Test
    @DisplayName("Should have correct @PageTitle")
    void shouldHaveCorrectPageTitle() {
        PageTitle pageTitle = SessionOverviewView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "SessionOverviewView must have @PageTitle annotation");
        assertEquals("Sessions", pageTitle.value());
    }

    @Test
    @DisplayName("Should import the hub stylesheet")
    void shouldImportStylesheet() {
        CssImport cssImport = SessionOverviewView.class.getAnnotation(CssImport.class);
        assertNotNull(cssImport, "SessionOverviewView must import a stylesheet");
        assertEquals("./hub-styles.css", cssImport.value());
    }

    @Test
    @DisplayName("Should render every visible session in the grid")
    void shouldRenderAllVisibleSessions() throws Exception {
        List<SessionOverview> sessions = List.of(
            overview("alice", "ACTIVE", 100.0, 0, "Pump module"),
            overview("bob", "ACTIVE", 45.0, 3, "Valve module"));
        when(visibilityService.listSessions()).thenReturn(sessions);
        when(visibilityService.userSummaries()).thenReturn(List.of(
            new UserSessionSummary("alice", 1, 0, 1),
            new UserSessionSummary("bob", 1, 1, 0)));

        SessionOverviewView view = createView();
        Grid<SessionOverview> grid = getField(view, "grid");
        String text = fullText(view.getElement());

        List<String> names = grid.getGenericDataView().getItems().map(SessionOverview::name).toList();
        assertEquals(2, names.size());
        assertTrue(names.contains("Pump module"));
        assertTrue(names.contains("Valve module"));
        assertTrue(text.contains("Sessions"));
        assertTrue(text.contains("alice \u2014 1 sessions, 0 incomplete, 1 complete"));
        assertTrue(text.contains("bob \u2014 1 sessions, 1 incomplete, 0 complete"));
    }

    @Test
    @DisplayName("Should render per-user summary chips")
    void shouldRenderPerUserChips() {
        when(visibilityService.listSessions()).thenReturn(List.of(
            overview("alice", "ACTIVE", 100.0, 0, "Pump module"),
            overview("alice", "ACTIVE", 50.0, 2, "Heater module"),
            overview(null, "ACTIVE", 10.0, 5, "Manual gap")));
        when(visibilityService.userSummaries()).thenReturn(List.of(
            new UserSessionSummary("alice", 2, 1, 1),
            new UserSessionSummary("Unassigned", 1, 1, 0)));

        SessionOverviewView view = createView();
        String text = fullText(view.getElement());

        assertTrue(text.contains("alice \u2014 2 sessions, 1 incomplete, 1 complete"));
        assertTrue(text.contains("Unassigned \u2014 1 sessions, 1 incomplete, 0 complete"));
    }

    @Test
    @DisplayName("Should filter the grid to incomplete sessions only")
    void shouldFilterByStatus() throws Exception {
        List<SessionOverview> sessions = List.of(
            overview("alice", "ACTIVE", 100.0, 0, "Pump module"),
            overview("bob", "ACTIVE", 45.0, 3, "Valve module"));
        when(visibilityService.listSessions()).thenReturn(sessions);
        when(visibilityService.userSummaries()).thenReturn(List.of());

        SessionOverviewView view = createView();
        com.vaadin.flow.component.combobox.ComboBox<String> statusFilter =
            getField(view, "statusFilter");
        statusFilter.setValue("Incomplete");
        Grid<SessionOverview> grid = getField(view, "grid");

        assertEquals(1, grid.getGenericDataView().getItems().count());
        SessionOverview visible = grid.getGenericDataView().getItems().findFirst().orElseThrow();
        assertEquals("bob", visible.owner());
    }

    @Test
    @DisplayName("Should filter the grid by user")
    void shouldFilterByUser() throws Exception {
        List<SessionOverview> sessions = List.of(
            overview("alice", "ACTIVE", 100.0, 0, "Pump module"),
            overview("bob", "ACTIVE", 45.0, 3, "Valve module"));
        when(visibilityService.listSessions()).thenReturn(sessions);
        when(visibilityService.userSummaries()).thenReturn(List.of());

        SessionOverviewView view = createView();
        com.vaadin.flow.component.combobox.ComboBox<String> userFilter =
            getField(view, "userFilter");
        userFilter.setValue("alice");
        Grid<SessionOverview> grid = getField(view, "grid");

        assertEquals(1, grid.getGenericDataView().getItems().count());
        assertEquals("alice", grid.getGenericDataView().getItems().findFirst().orElseThrow().owner());
    }

    @Test
    @DisplayName("Should filter the grid by free-text search on session name")
    void shouldFilterBySearch() throws Exception {
        List<SessionOverview> sessions = List.of(
            overview("alice", "ACTIVE", 100.0, 0, "Pump module"),
            overview("bob", "ACTIVE", 45.0, 3, "Valve module"));
        when(visibilityService.listSessions()).thenReturn(sessions);
        when(visibilityService.userSummaries()).thenReturn(List.of());

        SessionOverviewView view = createView();
        com.vaadin.flow.component.textfield.TextField searchField = getField(view, "searchField");
        searchField.setValue("pump");
        Grid<SessionOverview> grid = getField(view, "grid");

        assertEquals(1, grid.getGenericDataView().getItems().count());
        assertTrue(grid.getGenericDataView().getItems().findFirst().orElseThrow().name().contains("Pump"));
    }

    @Test
    @DisplayName("Should render Open/Archive/Delete actions for manageable sessions")
    void shouldRenderManageActions() throws Exception {
        SessionOverview session = overview("alice", "ACTIVE", 50.0, 2, "Pump module");
        when(visibilityService.listSessions()).thenReturn(List.of(session));
        when(visibilityService.userSummaries()).thenReturn(List.of());
        when(visibilityService.canManage(any())).thenReturn(true);

        SessionOverviewView view = createView();
        HorizontalLayout actions = invokeActions(view, session);
        List<Button> buttons = actions.getChildren().filter(Button.class::isInstance)
            .map(Button.class::cast).toList();

        assertEquals(3, buttons.size());
        assertEquals("Open", buttons.get(0).getText());
        assertEquals("Archive", buttons.get(1).getText());
        assertEquals("Delete", buttons.get(2).getText());
    }

    @Test
    @DisplayName("Should render only the Open action for sessions the caller may not manage")
    void shouldHideManageActionsForUnmanagedSessions() throws Exception {
        SessionOverview session = overview("bob", "ACTIVE", 50.0, 2, "Pump module");
        when(visibilityService.listSessions()).thenReturn(List.of(session));
        when(visibilityService.userSummaries()).thenReturn(List.of());
        when(visibilityService.canManage(any())).thenReturn(false);

        SessionOverviewView view = createView();
        HorizontalLayout actions = invokeActions(view, session);
        List<Button> buttons = actions.getChildren().filter(Button.class::isInstance)
            .map(Button.class::cast).toList();

        assertEquals(1, buttons.size());
        assertEquals("Open", buttons.get(0).getText());
    }

    @Test
    @DisplayName("Should resume the session when Open is triggered")
    void shouldResumeSessionOnOpen() throws Exception {
        SessionOverview session = overview("alice", "ACTIVE", 50.0, 2, "Pump module");
        when(visibilityService.listSessions()).thenReturn(List.of(session));
        when(visibilityService.userSummaries()).thenReturn(List.of());

        SessionOverviewView view = createView();
        Method openSession = SessionOverviewView.class.getDeclaredMethod(
            "openSession", SessionOverview.class);
        openSession.setAccessible(true);
        openSession.invoke(view, session);
        verify(hubService).resumeSession(session.id());
    }

    private static SessionOverview overview(String owner, String status, double coverage,
                                            int gaps, String name) {
        return new SessionOverview(UUID.randomUUID(), name, owner, status, coverage, gaps,
            1, 5, 2, 1, Instant.now(), Instant.now());
    }

    private static HorizontalLayout invokeActions(SessionOverviewView view, SessionOverview session)
        throws Exception {
        Method actions = SessionOverviewView.class.getDeclaredMethod("actions", SessionOverview.class);
        actions.setAccessible(true);
        return (HorizontalLayout) actions.invoke(view, session);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static String fullText(Element element) {
        StringBuilder builder = new StringBuilder();
        appendText(element, builder);
        return builder.toString();
    }

    private static void appendText(Element element, StringBuilder builder) {
        String own = element.getText();
        if (own != null && !own.isBlank()) {
            builder.append(own);
        }
        element.getChildren().forEach(child -> appendText(child, builder));
    }
}
