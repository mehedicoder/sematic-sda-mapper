package com.sda.mapper.hub.model;

/**
 * Per-user aggregate of Mapping Hub sessions for the session visibility view.
 */
public record UserSessionSummary(String owner, int total, int incomplete, int complete) {

    /**
     * Returns a summary with one more session added.
     *
     * @param completeSession whether the added session is complete
     * @return the incremented summary
     */
    public UserSessionSummary plus(boolean completeSession) {
        if (completeSession) {
            return new UserSessionSummary(owner, total + 1, incomplete, complete + 1);
        }
        return new UserSessionSummary(owner, total + 1, incomplete + 1, complete);
    }
}
