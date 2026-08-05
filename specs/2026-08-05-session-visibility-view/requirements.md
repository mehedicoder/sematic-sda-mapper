# Requirements: Session Visibility View — user-wise list of incomplete / complete mapping sessions

**Feature folder:** `specs/2026-08-05-session-visibility-view/`
**Proposed branch:** `feature/2026-08-05-session-visibility-view`
**Date:** 2026-08-05
**Status:** Proposed (not yet reviewed / validated)

## Problem Statement

Today a Mapping Hub session exists only inside a `@VaadinSessionScope` bean with a
random UUID (`MappingHubSession.java:26`). The persistence layer stores session rows
(`mapping_hub_session`), uploaded files, pipeline runs, canonical entities, instance
triples, matches and manifests — all keyed by `session_id` and optionally `user_id`
(`V2__mapping_hub_session_persistence.sql`). But:

- `user_id` is never populated, so no session is attributed to a user.
- No view lists sessions. Operators cannot see which sessions exist, who owns them,
  which are incomplete (gaps still open), or which are complete.
- Persisted sessions are never restored into a new `MappingHubSession` after a restart
  (`hydrateUnifiedGraph` is dead code), so an "incomplete" session cannot be resumed.

This feature adds a **Session Visibility View** that lists all sessions grouped per
user, marks each as **incomplete** or **complete**, and supports opening a session to
resume its work.

## Scope

### In Scope

1. **User attribution**
   - Capture the authenticated user (`Principal` / Vaadin authenticated user) on
     `MappingHubSession` and persist it into `mapping_hub_session.user_id`
     (column already exists).
   - Sessions without a user (tests, unauthenticated flows) are listed as "Unassigned".

2. **Completion semantics**
   - A session is **complete** when its latest pipeline run reports
     `coveragePercent == 100.0` **and** `blockingGapCount == 0`, or its status is
     `COMPLETED`. Everything else (ACTIVE, FAILED, or lower coverage) is **incomplete**.
   - The status is derived from persisted data (`mapping_hub_session` + latest
     `mapping_hub_pipeline_run`), not from live UI state.

3. **Session listing query**
   - `MappingHubPersistenceService.listSessions()` returning a `SessionOverview`
     DTO: session id, user id/name, session name, status, coverage percent, blocking
     gap count, uploaded file count, canonical entity count, semantic match count,
     manifest count, created/last-updated timestamps.
   - A second query returns per-user aggregate counts (total, incomplete, complete).

4. **Session Visibility View (`/sessions`) — separate view under Mapping Hub**
   - New, **separate** Vaadin view class `SessionOverviewView` with
     `@Route(value = "sessions", layout = MainLayout.class)` and
     `@PageTitle("Sessions")`. It is a distinct route/view, not a tab inside
     `MappingHubView`.
   - Navigation: placed **directly under the "Mapping Hub" item** in the
     `MainLayout` drawer (`MainLayout.createDrawerNavigation`), grouped with it
     (e.g. a "Mapping Hub" drawer group containing "Mapping Hub" and
     "Sessions", or the Sessions link immediately below the Mapping Hub link).
     This keeps the CSS-SDA hierarchy navigation intact while grouping the
     session visibility tool with the Mapping Hub feature.
   - A `Grid` grouped by user (expandable user rows → session rows).
   - Per-user summary chips: "N sessions, M incomplete, K complete".
   - Per-session row: name, owner, status pill (Incomplete/Complete), coverage %,
     blocking gaps, files, entities, matches, last activity, created.
   - Filters: status (Incomplete / Complete / All), user, free-text search on session
     name or id.
   - Actions per row: **Open / Resume** (loads persisted state back into the current
     session and navigates to the Mapping Hub), **Archive**, **Delete** (deletes the
     DB cascade, guarded by confirmation dialog).
   - Refresh button and lightweight auto-refresh timer.

5. **Session resume (restore path)**
   - Wire the previously dead `MappingHubGraphService.hydrateUnifiedGraph` /
     `MappingHubPersistenceService.loadTriples` path: opening a session re-hydrates the
     canonical model and inferred entities from persisted `canonical_entity` rows and
     instance triples, then re-runs the pipeline so coverage/matches are current.
   - After restart this makes persisted sessions (including manual entries) actually
     resumable — closing the gap identified in the session-persistence investigation.

6. **Security**
   - Authenticated users see their own sessions; `ADMIN` role sees all users.
   - Archive/Delete are admin-only; Open is available to the owner and admins.

7. **Tests**
   - Service tests for `listSessions`/user aggregates and complete-vs-incomplete
     classification (boundary: 100%+0 gaps complete, 99.9% incomplete, FAILED incomplete).
   - UI tests for `SessionOverviewView`: grouping by user, status pills, filters,
     open/archive/delete actions, admin-vs-owner visibility.
   - Resume test: persist entities + triples, restore into a fresh session, assert
     coverage is rebuilt.

### Out of Scope

- Multiple authentication providers / DB-backed users (stays with the existing
  in-memory `SecurityConfig`).
- Live collaboration or concurrent editing of the same session.
- Session retention/cleanup policies and archiving workflows beyond the single
  Archive/Delete actions.
- Editing/merging sessions, renaming, or cross-user session transfer.
- Real-time push of session progress.

## Decisions

- Completion is computed from persisted pipeline-run coverage, so the view reflects
  durable state and is identical across restarts.
- Grouping is done in the service layer (a flat `List<SessionOverview>` plus per-user
  aggregates) so the Vaadin grid stays simple and the data is reusable by a future REST
  endpoint.
- Resume reuses the existing `runStages` orchestration: restore into the session model,
  then re-run LIFT/INFER/MATCH so inferred entities and matches are recomputed from the
  restored base data.
- `user_id` is captured lazily on first pipeline activity from the authenticated
  principal; it is not a new auth flow.
- New SQL is added via a Flyway migration (never `ddl-auto=update`), following the
  project rule. `mapping_hub_session.user_id` already exists in V2, so only an index or
  helper changes may be needed.

## Context

- `MappingHubSession` (`@VaadinSessionScope`, random `UUID id`) — add `userId` +
  restore entry point.
- `MappingHubPersistenceService` — has write/load triples; needs `listSessions` +
  aggregates + `loadCanonicalEntities` (or reuse `canonical_entity` rows directly).
- `MappingHubService.hydrateUnifiedGraph()` exists but is never called
  (`MappingHubService.java:201`) — the resume path activates it.
- `SecurityConfig` provides `user` / `admin` with `USER` / `ADMIN` roles; Vaadin views
  currently have no role restrictions.
- `MainLayout.createDrawerNavigation` — add a drawer link (e.g. `VaadinIcon.ARCHIVE` /
  `VaadinIcon.GROUP`, "Sessions", `SessionOverviewView.class`).
- DB schema already models sessions, uploaded files, pipeline runs, canonical entities,
  triples, matches, manifests (V2 migration) — no schema redesign needed for the list.

## Functional Requirements

- **FR-1** Every persisted session is attributed to a user (or `NULL` → "Unassigned").
- **FR-2** The view lists all sessions the caller may see, grouped by user.
- **FR-3** Each session shows a status pill: **Incomplete** or **Complete**.
- **FR-4** The view supports filtering by Incomplete/Complete/All, by user, and by name/id search.
- **FR-5** Opening a session restores persisted canonical entities and triples into the
  current session, re-runs the pipeline, and navigates to the Mapping Hub.
- **FR-6** Admin can archive or delete a session; deleting cascades to all session data
  (existing `ON DELETE CASCADE`).
- **FR-7** The view is responsive (desktop, tablet, mobile) and follows the hub theme.

## Acceptance Criteria

- [ ] `SessionOverviewView` is a separate view at `/sessions` (own `@Route` + `@PageTitle`), navigated from the drawer under "Mapping Hub" — it is not a tab or sub-panel of `MappingHubView`.
- [ ] With two users and a mix of 100%-coverage and gap sessions, the view groups them
      per user and shows correct Incomplete/Complete pills.
- [ ] A session at exactly `coveragePercent == 100.0` with zero blocking gaps is
      Complete; anything else is Incomplete (including FAILED).
- [ ] Filtering by Incomplete / Complete / user / search returns the expected rows.
- [ ] Opening an archived-or-persisted session from another browser session restores its
      entities and manual entries and recomputes coverage (survives app restart).
- [ ] Non-admin users cannot see or act on other users' sessions.
- [ ] Archive/Delete require a confirmation dialog; Delete removes all session data.
- [ ] `mvn clean test` — BUILD SUCCESS, all new and existing tests pass.
- [ ] New `SessionOverviewView` UI test covers rendering, grouping, filters and actions.

## Data Model Changes (Flyway V4)

- `mapping_hub_session.user_id` already exists (V2); add helper indexes if needed:
  `CREATE INDEX idx_mapping_hub_session_owner_updated ON mapping_hub_session(user_id, updated_at DESC);`
- No other schema changes are required for listing.

## Artifact Placement

- Spec folder: `specs/2026-08-05-session-visibility-view/`
- Reports: `spec-review-report.md`, `spec-validation-report.md`, `review-report.md`,
  `validation-report.md`
- Roadmap: new unstarted entry under Phase 11c (Mapping Hub) in `specs/roadmap.md`
