package com.ims.workflow.state;

import com.ims.model.enums.IncidentState;
import com.ims.model.postgres.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Handles transitions from the INVESTIGATING state.
 * Valid: INVESTIGATING → RESOLVED, INVESTIGATING → OPEN (reopen/escalate)
 */
@Component
public class InvestigatingStateHandler implements IncidentStateHandler {

    private static final Logger log = LoggerFactory.getLogger(InvestigatingStateHandler.class);

    @Override
    public boolean investigate(Incident incident) {
        return false; // Already investigating
    }

    @Override
    public boolean resolve(Incident incident) {
        log.info("Transitioning incident {} from INVESTIGATING to RESOLVED", incident.getId());
        incident.setState(IncidentState.RESOLVED);
        incident.setResolvedAt(Instant.now());
        // Calculate MTTR: resolved_at - first_signal_at
        if (incident.getFirstSignalAt() != null) {
            long mttr = java.time.Duration.between(incident.getFirstSignalAt(), incident.getResolvedAt()).getSeconds();
            incident.setMttrSeconds(mttr);
        }
        return true;
    }

    @Override
    public boolean close(Incident incident) {
        return false; // Must resolve before closing
    }

    @Override
    public boolean reopen(Incident incident) {
        log.info("Reopening incident {} from INVESTIGATING to OPEN", incident.getId());
        incident.setState(IncidentState.OPEN);
        return true;
    }

    @Override
    public IncidentState getState() {
        return IncidentState.INVESTIGATING;
    }
}
