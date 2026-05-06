package com.ims.workflow.state;

import com.ims.model.enums.IncidentState;
import com.ims.model.postgres.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handles transitions from the OPEN state.
 * Valid: OPEN → INVESTIGATING
 */
@Component
public class OpenStateHandler implements IncidentStateHandler {

    private static final Logger log = LoggerFactory.getLogger(OpenStateHandler.class);

    @Override
    public boolean investigate(Incident incident) {
        log.info("Transitioning incident {} from OPEN to INVESTIGATING", incident.getId());
        incident.setState(IncidentState.INVESTIGATING);
        return true;
    }

    @Override
    public boolean resolve(Incident incident) {
        return false; // Cannot resolve directly from OPEN
    }

    @Override
    public boolean close(Incident incident) {
        return false; // Cannot close directly from OPEN
    }

    @Override
    public boolean reopen(Incident incident) {
        return false; // Already open
    }

    @Override
    public IncidentState getState() {
        return IncidentState.OPEN;
    }
}
