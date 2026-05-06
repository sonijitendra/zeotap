package com.ims.workflow.state;

import com.ims.exception.InvalidStateTransitionException;
import com.ims.model.enums.IncidentState;
import com.ims.model.postgres.Incident;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * State machine that dispatches transition requests to the correct state handler.
 * Uses an EnumMap for O(1) handler lookup.
 */
@Component
public class IncidentStateMachine {

    private static final Logger log = LoggerFactory.getLogger(IncidentStateMachine.class);

    private final List<IncidentStateHandler> handlers;
    private final Map<IncidentState, IncidentStateHandler> handlerMap = new EnumMap<>(IncidentState.class);

    public IncidentStateMachine(List<IncidentStateHandler> handlers) {
        this.handlers = handlers;
    }

    @PostConstruct
    void init() {
        handlers.forEach(h -> handlerMap.put(h.getState(), h));
        log.info("Initialized incident state machine with {} state handlers", handlerMap.size());
    }

    /**
     * Transitions an incident to the target state.
     * Throws InvalidStateTransitionException if the transition is not permitted.
     */
    public void transition(Incident incident, IncidentState targetState) {
        IncidentState currentState = incident.getState();
        IncidentStateHandler handler = handlerMap.get(currentState);

        if (handler == null) {
            throw new InvalidStateTransitionException(currentState.name(), targetState.name());
        }

        boolean success = switch (targetState) {
            case INVESTIGATING -> handler.investigate(incident);
            case RESOLVED -> handler.resolve(incident);
            case CLOSED -> handler.close(incident);
            case OPEN -> handler.reopen(incident);
        };

        if (!success) {
            throw new InvalidStateTransitionException(currentState.name(), targetState.name());
        }

        log.info("Incident {} transitioned: {} → {}", incident.getId(), currentState, targetState);
    }
}
