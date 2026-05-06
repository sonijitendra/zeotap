package com.ims.workflow;

import com.ims.exception.InvalidStateTransitionException;
import com.ims.model.enums.IncidentState;
import com.ims.model.enums.Severity;
import com.ims.model.postgres.Incident;
import com.ims.workflow.state.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Incident State Machine Tests")
class IncidentStateMachineTest {

    private IncidentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        List<IncidentStateHandler> handlers = List.of(
                new OpenStateHandler(),
                new InvestigatingStateHandler(),
                new ResolvedStateHandler(),
                new ClosedStateHandler()
        );
        stateMachine = new IncidentStateMachine(handlers);
        stateMachine.init();
    }

    private Incident createIncident(IncidentState state) {
        return Incident.builder()
                .id(UUID.randomUUID()).componentId("TEST_COMP")
                .severity(Severity.P1).state(state)
                .title("Test Incident").signalCount(1)
                .firstSignalAt(Instant.now()).build();
    }

    @Nested
    @DisplayName("Valid Transitions")
    class ValidTransitions {
        @Test
        @DisplayName("OPEN → INVESTIGATING")
        void openToInvestigating() {
            Incident incident = createIncident(IncidentState.OPEN);
            stateMachine.transition(incident, IncidentState.INVESTIGATING);
            assertEquals(IncidentState.INVESTIGATING, incident.getState());
        }

        @Test
        @DisplayName("INVESTIGATING → RESOLVED with MTTR calculation")
        void investigatingToResolved() {
            Incident incident = createIncident(IncidentState.INVESTIGATING);
            incident.setFirstSignalAt(Instant.now().minusSeconds(300));
            stateMachine.transition(incident, IncidentState.RESOLVED);
            assertEquals(IncidentState.RESOLVED, incident.getState());
            assertNotNull(incident.getResolvedAt());
            assertNotNull(incident.getMttrSeconds());
            assertTrue(incident.getMttrSeconds() >= 300);
        }

        @Test
        @DisplayName("RESOLVED → CLOSED")
        void resolvedToClosed() {
            Incident incident = createIncident(IncidentState.RESOLVED);
            stateMachine.transition(incident, IncidentState.CLOSED);
            assertEquals(IncidentState.CLOSED, incident.getState());
            assertNotNull(incident.getClosedAt());
        }

        @Test
        @DisplayName("INVESTIGATING → OPEN (reopen)")
        void investigatingToOpen() {
            Incident incident = createIncident(IncidentState.INVESTIGATING);
            stateMachine.transition(incident, IncidentState.OPEN);
            assertEquals(IncidentState.OPEN, incident.getState());
        }
    }

    @Nested
    @DisplayName("Invalid Transitions")
    class InvalidTransitions {
        @Test
        @DisplayName("OPEN → RESOLVED should throw")
        void openToResolved() {
            Incident incident = createIncident(IncidentState.OPEN);
            assertThrows(InvalidStateTransitionException.class,
                    () -> stateMachine.transition(incident, IncidentState.RESOLVED));
        }

        @Test
        @DisplayName("OPEN → CLOSED should throw")
        void openToClosed() {
            Incident incident = createIncident(IncidentState.OPEN);
            assertThrows(InvalidStateTransitionException.class,
                    () -> stateMachine.transition(incident, IncidentState.CLOSED));
        }

        @Test
        @DisplayName("CLOSED → any should throw")
        void closedIsTerminal() {
            Incident incident = createIncident(IncidentState.CLOSED);
            assertThrows(InvalidStateTransitionException.class,
                    () -> stateMachine.transition(incident, IncidentState.OPEN));
            assertThrows(InvalidStateTransitionException.class,
                    () -> stateMachine.transition(incident, IncidentState.INVESTIGATING));
        }
    }
}
