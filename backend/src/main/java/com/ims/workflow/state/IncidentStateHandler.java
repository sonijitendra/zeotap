package com.ims.workflow.state;

import com.ims.model.postgres.Incident;

/**
 * State Pattern interface for incident lifecycle management.
 * Each concrete state defines which transitions are valid and what
 * side-effects occur during a transition.
 */
public interface IncidentStateHandler {

    /**
     * Attempt to transition the incident to the INVESTIGATING state.
     * @return true if the transition was applied
     */
    boolean investigate(Incident incident);

    /**
     * Attempt to transition the incident to the RESOLVED state.
     * @return true if the transition was applied
     */
    boolean resolve(Incident incident);

    /**
     * Attempt to transition the incident to the CLOSED state.
     * Requires a completed RCA.
     * @return true if the transition was applied
     */
    boolean close(Incident incident);

    /**
     * Attempt to reopen the incident (transition back to a prior state).
     * @return true if the transition was applied
     */
    boolean reopen(Incident incident);

    /**
     * @return the IncidentState enum value this handler manages
     */
    com.ims.model.enums.IncidentState getState();
}
