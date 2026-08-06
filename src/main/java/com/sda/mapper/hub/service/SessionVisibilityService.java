package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.SessionOverview;
import com.sda.mapper.hub.model.UserSessionSummary;
import com.sda.mapper.hub.persistence.MappingHubPersistenceService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides Mapping Hub session visibility data scoped to the current user's
 * permissions. Authenticated users see their own sessions; administrators see
 * all sessions. Sessions without an owner are shown as "Unassigned".
 */
@Service
public class SessionVisibilityService {

    private final MappingHubPersistenceService persistenceService;

    /**
     * Creates the visibility service.
     *
     * @param persistenceService durable session storage
     */
    public SessionVisibilityService(MappingHubPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    /**
     * Returns the sessions the current user may see, most recently updated
     * first.
     *
     * @return visible session overviews
     */
    public List<SessionOverview> listSessions() {
        if (persistenceService == null) {
            return List.of();
        }
        List<SessionOverview> all = persistenceService.listSessions();
        if (CurrentUser.isAdmin()) {
            return all;
        }
        String owner = CurrentUser.username().orElse(null);
        return all.stream()
            .filter(s -> s.owner() == null || (owner != null && s.owner().equals(owner)))
            .toList();
    }

    /**
     * Returns per-user aggregate counts for the visible sessions, grouped in
     * first-seen order. Sessions without an owner are grouped under
     * "Unassigned".
     *
     * @return user summaries
     */
    public List<UserSessionSummary> userSummaries() {
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (SessionOverview overview : listSessions()) {
            String owner = overview.owner() == null ? "Unassigned" : overview.owner();
            int[] entry = counts.computeIfAbsent(owner, k -> new int[3]);
            entry[0]++;
            if (overview.complete()) {
                entry[2]++;
            } else {
                entry[1]++;
            }
        }
        return counts.entrySet().stream()
            .map(e -> new UserSessionSummary(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
            .toList();
    }

    /**
     * Returns whether the current user may act on (archive or delete) the given
     * session. Admins may act on every session; owners may act on their own.
     *
     * @param overview the session overview
     * @return {@code true} when the caller may manage the session
     */
    public boolean canManage(SessionOverview overview) {
        if (CurrentUser.isAdmin()) {
            return true;
        }
        String owner = CurrentUser.username().orElse(null);
        return owner != null && owner.equals(overview.owner());
    }

    /**
     * Archives the given session when the caller is allowed to manage it.
     *
     * @param overview the session overview
     */
    public void archive(SessionOverview overview) {
        if (persistenceService != null && canManage(overview)) {
            persistenceService.archiveSession(overview.id());
        }
    }

    /**
     * Deletes the given session and all cascaded data when the caller is
     * allowed to manage it.
     *
     * @param overview the session overview
     */
    public void delete(SessionOverview overview) {
        if (persistenceService != null && canManage(overview)) {
            persistenceService.deleteSessionData(overview.id());
        }
    }
}
