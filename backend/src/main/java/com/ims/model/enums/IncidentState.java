package com.ims.model.enums;

/**
 * Incident lifecycle states following the State Pattern.
 * Transitions: OPEN → INVESTIGATING → RESOLVED → CLOSED
 */
public enum IncidentState {
    OPEN,
    INVESTIGATING,
    RESOLVED,
    CLOSED;

    /**
     * Validates whether a transition from this state to the target state is permitted.
     */
    public boolean canTransitionTo(IncidentState target) {
        return switch (this) {
            case OPEN -> target == INVESTIGATING;
            case INVESTIGATING -> target == RESOLVED || target == OPEN;
            case RESOLVED -> target == CLOSED || target == INVESTIGATING;
            case CLOSED -> false; // Terminal state
        };
    }
}
