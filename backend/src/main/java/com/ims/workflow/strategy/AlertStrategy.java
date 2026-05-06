package com.ims.workflow.strategy;

import com.ims.model.postgres.Incident;

/**
 * Strategy Pattern interface for severity-specific alerting behavior.
 * Each severity level (P0, P1, P2) has its own notification and escalation logic.
 */
public interface AlertStrategy {

    /**
     * Execute the alerting logic for the given incident.
     */
    void alert(Incident incident);

    /**
     * @return the severity level this strategy handles
     */
    String getSeverity();
}
