package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.SessionOverview;
import com.sda.mapper.hub.model.UserSessionSummary;
import com.sda.mapper.hub.service.MappingHubService;
import com.sda.mapper.hub.service.SessionResumeException;
import com.sda.mapper.hub.service.SessionVisibilityService;
import com.sda.mapper.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Session Visibility View: lists all Mapping Hub sessions the caller may see,
 * grouped per user with incomplete/complete status, per-user summary chips and
 * status/user/search filters. Supports opening (resuming) a persisted session
 * into the Mapping Hub and, for authorized users, archiving or deleting it.
 */
@Route(value = "sessions", layout = MainLayout.class)
@PageTitle("Sessions")
@CssImport("./hub-styles.css")
public class SessionOverviewView extends VerticalLayout {

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final SessionVisibilityService visibilityService;
    private final MappingHubService hubService;

    private final Grid<SessionOverview> grid = new Grid<>();
    private final HorizontalLayout chips = new HorizontalLayout();
    private final ComboBox<String> statusFilter = new ComboBox<>("Status");
    private final ComboBox<String> userFilter = new ComboBox<>("User");
    private final TextField searchField = new TextField("Search");
    private List<SessionOverview> sessions = List.of();
    private ScheduledExecutorService refresher;

    /**
     * Creates the session visibility view.
     *
     * @param visibilityService session listing scoped to the current user
     * @param hubService        mapping hub pipeline used to resume sessions
     */
    public SessionOverviewView(SessionVisibilityService visibilityService, MappingHubService hubService) {
        this.visibilityService = visibilityService;
        this.hubService = hubService;

        addClassName("hub-session-view");
        setPadding(true);

        add(createHeader());
        add(createFilterBar());
        add(createChips());
        add(createGrid());

        getElement().addAttachListener(e -> startAutoRefresh());
        getElement().addDetachListener(e -> stopAutoRefresh());

        refresh();
    }

    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout titles = new VerticalLayout();
        titles.setPadding(false);
        titles.setSpacing(false);
        H3 title = new H3("Sessions");
        title.addClassName("hub-section-label");
        title.getStyle().set("margin", "0");
        Span subtitle = new Span("Mapping Hub sessions per user \u2014 incomplete and complete");
        subtitle.getStyle().set("color", "var(--hub-text-3)");
        titles.add(title, subtitle);

        Button refresh = new Button("Refresh", e -> refresh());
        refresh.addClassName("hub-manual-toggle");
        header.add(titles, refresh);
        return header;
    }

    private Component createFilterBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.END);

        statusFilter.setItems("All", "Incomplete", "Complete");
        statusFilter.setValue("All");
        statusFilter.addValueChangeListener(e -> renderGrid());

        userFilter.setItems("All users");
        userFilter.setValue("All users");
        userFilter.addValueChangeListener(e -> renderGrid());

        searchField.setPlaceholder("Search session name or id...");
        searchField.setClearButtonVisible(true);
        searchField.addValueChangeListener(e -> renderGrid());

        bar.add(statusFilter, userFilter, searchField);
        return bar;
    }

    private Component createChips() {
        chips.setWidthFull();
        chips.setSpacing(true);
        chips.setPadding(true);
        chips.addClassName("hub-session-chips");
        return chips;
    }

    private Component createGrid() {
        grid.setWidthFull();
        grid.setHeight("600px");
        grid.setAllRowsVisible(true);

        grid.addColumn(s -> s.owner() == null ? "Unassigned" : s.owner())
            .setHeader("User")
            .setSortable(true)
            .setKey("owner");
        grid.addColumn(SessionOverview::name)
            .setHeader("Session")
            .setSortable(true)
            .setKey("name");
        grid.addComponentColumn(this::statusPill)
            .setHeader("Status")
            .setKey("status");
        grid.addColumn(s -> String.format("%.1f%%", s.coveragePercent()))
            .setHeader("Coverage")
            .setSortable(true)
            .setKey("coverage");
        grid.addColumn(SessionOverview::blockingGapCount)
            .setHeader("Gaps")
            .setSortable(true)
            .setKey("gaps");
        grid.addColumn(SessionOverview::fileCount)
            .setHeader("Files")
            .setKey("files");
        grid.addColumn(SessionOverview::entityCount)
            .setHeader("Entities")
            .setKey("entities");
        grid.addColumn(SessionOverview::matchCount)
            .setHeader("Matches")
            .setKey("matches");
        grid.addColumn(SessionOverview::manifestCount)
            .setHeader("Manifests")
            .setKey("manifests");
        grid.addColumn(s -> TIME_FORMAT.format(s.updatedAt()))
            .setHeader("Last activity")
            .setSortable(true)
            .setKey("updated");
        grid.addComponentColumn(this::actions)
            .setHeader("Actions")
            .setKey("actions");

        return grid;
    }

    private Component statusPill(SessionOverview session) {
        Span pill = new Span(session.complete() ? "Complete" : "Incomplete");
        pill.addClassName("hub-status-pill");
        pill.addClassName(session.complete() ? "hub-status--present" : "hub-status--missing");
        return pill;
    }

    private Component actions(SessionOverview session) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setPadding(false);

        Button open = new Button("Open", e -> openSession(session));
        open.addClassName("hub-manual-toggle");
        row.add(open);

        if (visibilityService.canManage(session)) {
            if (!"ARCHIVED".equals(session.status())) {
                Button archive = new Button("Archive", e -> confirmArchive(session));
                archive.addClassName("hub-manual-toggle");
                row.add(archive);
            }
            Button delete = new Button("Delete", e -> confirmDelete(session));
            delete.addClassName("hub-manual-toggle");
            row.add(delete);
        }
        return row;
    }

    private void openSession(SessionOverview session) {
        try {
            hubService.resumeSession(session.id());
            notifyUser("Resumed session " + session.name(), 3000);
            getUI().ifPresent(ui -> ui.navigate(MappingHubView.class));
        } catch (SessionResumeException e) {
            notifyUser("Could not resume session: " + e.getMessage(), 5000);
        }
    }

    private void notifyUser(String message, int duration) {
        getUI().ifPresent(ui -> ui.access(() ->
            Notification.show(message, duration, Notification.Position.TOP_CENTER)));
    }

    private void confirmArchive(SessionOverview session) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Archive session");
        dialog.setText("Archive \"" + session.name() + "\"? The session is marked as archived but remains "
            + "visible and openable.");
        dialog.setCancelable(true);
        dialog.setRejectable(false);
        dialog.setConfirmText("Archive");
        dialog.addConfirmListener(e -> {
            visibilityService.archive(session);
            Notification.show("Session archived", 2000, Notification.Position.TOP_CENTER);
            refresh();
        });
        dialog.open();
    }

    private void confirmDelete(SessionOverview session) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete session");
        dialog.setText("Delete \"" + session.name() + "\"? All uploaded files, entities, triples, matches and "
            + "manifests of this session are removed permanently.");
        dialog.setCancelable(true);
        dialog.setRejectable(false);
        dialog.setConfirmText("Delete");
        dialog.addConfirmListener(e -> {
            visibilityService.delete(session);
            Notification.show("Session deleted", 2000, Notification.Position.TOP_CENTER);
            refresh();
        });
        dialog.open();
    }

    private void refresh() {
        sessions = visibilityService.listSessions();
        List<String> users = sessions.stream()
            .map(s -> s.owner() == null ? "Unassigned" : s.owner())
            .distinct()
            .sorted()
            .toList();
        String previousUser = userFilter.getValue();
        userFilter.setItems(java.util.stream.Stream.concat(
            java.util.stream.Stream.of("All users"), users.stream()).toList());
        userFilter.setValue(users.contains(previousUser) ? previousUser : "All users");
        renderChips();
        renderGrid();
    }

    private void renderChips() {
        chips.removeAll();
        List<UserSessionSummary> summaries = visibilityService.userSummaries();
        if (summaries.isEmpty()) {
            chips.add(new Span("No sessions yet."));
            return;
        }
        for (UserSessionSummary summary : summaries) {
            Span chip = new Span(summary.owner() + " \u2014 " + summary.total() + " sessions, "
                + summary.incomplete() + " incomplete, " + summary.complete() + " complete");
            chip.addClassName("hub-session-chip");
            chips.add(chip);
        }
    }

    private void renderGrid() {
        List<SessionOverview> filtered = sessions.stream()
            .filter(this::matchesFilters)
            .toList();
        grid.setItems(filtered);
    }

    private boolean matchesFilters(SessionOverview session) {
        String status = statusFilter.getValue();
        if ("Incomplete".equals(status) && session.complete()) {
            return false;
        }
        if ("Complete".equals(status) && !session.complete()) {
            return false;
        }
        String user = userFilter.getValue();
        if (user != null && !"All users".equals(user)) {
            String owner = session.owner() == null ? "Unassigned" : session.owner();
            if (!user.equals(owner)) {
                return false;
            }
        }
        String query = searchField.getValue() == null ? "" : searchField.getValue().trim().toLowerCase();
        if (!query.isEmpty()) {
            String name = session.name() == null ? "" : session.name().toLowerCase();
            String id = session.id().toString();
            if (!name.contains(query) && !id.contains(query)) {
                return false;
            }
        }
        return true;
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        refresher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "session-overview-refresh");
            thread.setDaemon(true);
            return thread;
        });
        refresher.scheduleAtFixedRate(() -> getUI().ifPresent(ui -> ui.access(this::refresh)),
            30, 30, TimeUnit.SECONDS);
    }

    private void stopAutoRefresh() {
        if (refresher != null) {
            refresher.shutdownNow();
            refresher = null;
        }
    }
}
