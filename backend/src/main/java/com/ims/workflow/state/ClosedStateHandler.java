package com.ims.workflow.state;

import com.ims.model.enums.IncidentState;
import com.ims.model.postgres.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CLOSED is a terminal state. No transitions are permitted.
 */
@Component
public class ClosedStateHandler implements IncidentStateHandler {

    private static final Logger log = LoggerFactory.getLogger(ClosedStateHandler.class);

    @Override
    public boolean investigate(Incident incident) {
        log.warn("Cannot transition CLOSED incident {} — terminal state", incident.getId());
        return false;
    }

    @Override
    public boolean resolve(Incident incident) {
        return false;
    }

    @Override
    public boolean close(Incident incident) {
        return false;
    }

    @Override
    public boolean reopen(Incident incident) {
        return false; // CLOSED is final
    }

    @Override
    public IncidentState getState() {
        return IncidentState.CLOSED;
    }
}
