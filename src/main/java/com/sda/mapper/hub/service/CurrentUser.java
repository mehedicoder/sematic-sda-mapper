package com.sda.mapper.hub.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Resolves the currently authenticated user from the Spring Security context.
 *
 * <p>Used to attribute Mapping Hub sessions to their owning user and to scope
 * session visibility to the caller's permissions.</p>
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /**
     * Returns the authenticated user name, if any.
     *
     * @return the principal name, or an empty optional when unauthenticated
     */
    public static Optional<String> username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return Optional.of(userDetails.getUsername());
        }
        if ("anonymousUser".equals(principal)) {
            return Optional.empty();
        }
        return Optional.ofNullable(auth.getName());
    }

    /**
     * Returns whether the current user holds the ADMIN role.
     *
     * @return {@code true} for administrators
     */
    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
            .anyMatch(granted -> "ROLE_ADMIN".equals(granted.getAuthority()));
    }
}
