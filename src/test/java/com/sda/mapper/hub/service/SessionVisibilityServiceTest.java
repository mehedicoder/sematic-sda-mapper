package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.SessionOverview;
import com.sda.mapper.hub.model.UserSessionSummary;
import com.sda.mapper.hub.persistence.MappingHubPersistenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Session visibility service")
class SessionVisibilityServiceTest {

    private final MappingHubPersistenceService persistence = mock(MappingHubPersistenceService.class);
    private final SessionVisibilityService service = new SessionVisibilityService(persistence);

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should classify 100% coverage with zero blocking gaps as complete")
    void shouldClassifyFullCoverageAsComplete() {
        assertTrue(overview("alice", "ACTIVE", 100.0, 0).complete());
    }

    @Test
    @DisplayName("Should classify 99.9% coverage as incomplete")
    void shouldClassifyBelowFullCoverageAsIncomplete() {
        assertFalse(overview("alice", "ACTIVE", 99.9, 0).complete());
    }

    @Test
    @DisplayName("Should classify a failed run as incomplete regardless of coverage")
    void shouldClassifyFailedRunAsIncomplete() {
        assertFalse(overview("alice", "FAILED", 100.0, 0).complete());
    }

    @Test
    @DisplayName("Should classify an explicitly completed run as complete regardless of coverage")
    void shouldClassifyCompletedStatusAsComplete() {
        assertTrue(overview("alice", "COMPLETED", 0.0, 5).complete());
    }

    @Test
    @DisplayName("Should let a non-admin user see own and unassigned sessions only")
    void shouldScopeNonAdminToOwnAndUnassigned() {
        login("alice", "ROLE_USER");
        when(persistence.listSessions()).thenReturn(List.of(
            overview("alice", "ACTIVE", 100.0, 0),
            overview("bob", "ACTIVE", 50.0, 2),
            overview(null, "ACTIVE", 20.0, 4)));

        List<SessionOverview> visible = service.listSessions();

        assertEquals(2, visible.size());
        assertTrue(visible.stream().anyMatch(s -> "alice".equals(s.owner())));
        assertTrue(visible.stream().anyMatch(s -> s.owner() == null));
        assertTrue(visible.stream().noneMatch(s -> "bob".equals(s.owner())));
    }

    @Test
    @DisplayName("Should hide other users' sessions from an unauthenticated caller")
    void shouldHideOwnedSessionsFromUnauthenticatedCaller() {
        SecurityContextHolder.clearContext();
        when(persistence.listSessions()).thenReturn(List.of(
            overview("alice", "ACTIVE", 100.0, 0),
            overview(null, "ACTIVE", 20.0, 4)));

        List<SessionOverview> visible = service.listSessions();

        assertEquals(1, visible.size());
        assertTrue(visible.get(0).owner() == null);
    }

    @Test
    @DisplayName("Should let an admin see every session")
    void shouldLetAdminSeeEverySession() {
        login("admin", "ROLE_ADMIN");
        when(persistence.listSessions()).thenReturn(List.of(
            overview("alice", "ACTIVE", 100.0, 0),
            overview("bob", "ACTIVE", 50.0, 2)));

        List<SessionOverview> visible = service.listSessions();

        assertEquals(2, visible.size());
    }

    @Test
    @DisplayName("Should aggregate per-user counts including the Unassigned bucket")
    void shouldAggregatePerUserCounts() {
        login("admin", "ROLE_ADMIN");
        when(persistence.listSessions()).thenReturn(List.of(
            overview("alice", "ACTIVE", 100.0, 0),
            overview("alice", "ACTIVE", 50.0, 2),
            overview("bob", "ACTIVE", 40.0, 3),
            overview(null, "ACTIVE", 0.0, 1)));

        List<UserSessionSummary> summaries = service.userSummaries();

        assertEquals(3, summaries.size());
        UserSessionSummary alice = summaries.stream()
            .filter(s -> "alice".equals(s.owner())).findFirst().orElseThrow();
        assertEquals(2, alice.total());
        assertEquals(1, alice.incomplete());
        assertEquals(1, alice.complete());
        UserSessionSummary unassigned = summaries.stream()
            .filter(s -> "Unassigned".equals(s.owner())).findFirst().orElseThrow();
        assertEquals(1, unassigned.total());
        assertEquals(1, unassigned.incomplete());
        assertEquals(0, unassigned.complete());
    }

    @Test
    @DisplayName("Should allow an admin to manage every session")
    void shouldLetAdminManageEverySession() {
        login("admin", "ROLE_ADMIN");
        assertTrue(service.canManage(overview("bob", "ACTIVE", 50.0, 2)));
    }

    @Test
    @DisplayName("Should let an owner manage own session but not another user's")
    void shouldScopeManagementToOwner() {
        login("alice", "ROLE_USER");
        assertTrue(service.canManage(overview("alice", "ACTIVE", 50.0, 2)));
        assertFalse(service.canManage(overview("bob", "ACTIVE", 50.0, 2)));
    }

    @Test
    @DisplayName("Should archive and delete a session the caller may manage")
    void shouldArchiveAndDeleteManagedSession() {
        login("alice", "ROLE_USER");
        SessionOverview own = overview("alice", "ACTIVE", 50.0, 2);

        service.archive(own);
        service.delete(own);

        org.mockito.Mockito.verify(persistence).archiveSession(own.id());
        org.mockito.Mockito.verify(persistence).deleteSessionData(own.id());
    }

    @Test
    @DisplayName("Should not archive or delete a session the caller may not manage")
    void shouldNotTouchUnmanagedSession() {
        login("alice", "ROLE_USER");
        SessionOverview other = overview("bob", "ACTIVE", 50.0, 2);

        service.archive(other);
        service.delete(other);

        org.mockito.Mockito.verifyNoInteractions(persistence);
    }

    private static SessionOverview overview(String owner, String status, double coverage, int gaps) {
        return new SessionOverview(UUID.randomUUID(), "Session", owner, status, coverage, gaps,
            1, 5, 2, 1, Instant.now(), Instant.now());
    }

    private static void login(String username, String role) {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, "password",
                List.of(new SimpleGrantedAuthority(role))));
    }
}
