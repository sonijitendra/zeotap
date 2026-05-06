package com.ims.workflow.state;

import com.ims.model.enums.IncidentState;
import com.ims.model.postgres.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Handles transitions from the RESOLVED state.
 * Valid: RESOLVED → CLOSED (requires RCA), RESOLVED → INVESTIGATING (reopen)
 */
@Component
public class ResolvedStateHandler implements IncidentStateHandler {

    private static final Logger log = LoggerFactory.getLogger(ResolvedStateHandler.class);

    @Override
    public boolean investigate(Incident incident) {
        log.info("Re-investigating incident {} (RESOLVED → INVESTIGATING)", incident.getId());
        incident.setState(IncidentState.INVESTIGATING);
        incident.setResolvedAt(null);
        incident.setMttrSeconds(null);
        return true;
    }

    @Override
    public boolean resolve(Incident incident) {
        return false; // Already resolved
    }

    @Override
    public boolean close(Incident incident) {
        // RCA validation is performed at the service layer
        log.info("Closing incident {} (RESOLVED → CLOSED)", incident.getId());
        incident.setState(IncidentState.CLOSED);
        incident.setClosedAt(Instant.now());
        return true;
    }

    @Override
    public boolean reopen(Incident incident) {
        return investigate(incident); // Reopen goes back to investigating
    }

    @Override
    public IncidentState getState() {
        return IncidentState.RESOLVED;
    }
}
