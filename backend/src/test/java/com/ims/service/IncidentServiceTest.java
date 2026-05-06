package com.ims.service;

import com.ims.dto.request.StateTransitionRequest;
import com.ims.dto.response.IncidentResponse;
import com.ims.exception.InvalidStateTransitionException;
import com.ims.exception.RcaRequiredException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.model.enums.IncidentState;
import com.ims.model.enums.Severity;
import com.ims.model.postgres.Incident;
import com.ims.repository.postgres.IncidentRepository;
import com.ims.repository.postgres.RcaRepository;
import com.ims.repository.postgres.TimelineRepository;
import com.ims.workflow.state.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Incident Service Tests")
class IncidentServiceTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private TimelineRepository timelineRepository;
    @Mock private RcaRepository rcaRepository;
    @Mock private SseEmitterService sseEmitterService;
    @Mock private AuditService auditService;

    private IncidentService incidentService;
    private IncidentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        List<IncidentStateHandler> handlers = List.of(
                new OpenStateHandler(), new InvestigatingStateHandler(),
                new ResolvedStateHandler(), new ClosedStateHandler()
        );
        stateMachine = new IncidentStateMachine(handlers);
        stateMachine.init();
        incidentService = new IncidentService(
                incidentRepository, timelineRepository, rcaRepository,
                stateMachine, sseEmitterService, auditService);
    }

    private Incident createIncident(IncidentState state) {
        return Incident.builder()
                .id(UUID.randomUUID()).componentId("API_GATEWAY")
                .severity(Severity.P1).state(state)
                .title("Test Incident").signalCount(3)
                .firstSignalAt(Instant.now().minusSeconds(120)).version(0).build();
    }

    @Nested
    @DisplayName("Get Incident")
    class GetIncident {
        @Test
        @DisplayName("Returns incident response for valid ID")
        void returnsIncident() {
            Incident incident = createIncident(IncidentState.OPEN);
            when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
            when(rcaRepository.existsByIncidentId(incident.getId())).thenReturn(false);

            IncidentResponse response = incidentService.getIncident(incident.getId());

            assertEquals(incident.getId(), response.getId());
            assertEquals("API_GATEWAY", response.getComponentId());
            assertFalse(response.isHasRca());
        }

        @Test
        @DisplayName("Throws for non-existent incident")
        void throwsForMissing() {
            UUID id = UUID.randomUUID();
            when(incidentRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> incidentService.getIncident(id));
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {
        @Test
        @DisplayName("Valid OPEN → INVESTIGATING transition")
        void validTransition() {
            Incident incident = createIncident(IncidentState.OPEN);
            when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
            when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(rcaRepository.existsByIncidentId(incident.getId())).thenReturn(false);

            StateTransitionRequest request = StateTransitionRequest.builder()
                    .targetState("INVESTIGATING").changedBy("operator").build();

            IncidentResponse response = incidentService.transitionState(incident.getId(), request);

            assertEquals(IncidentState.INVESTIGATING, response.getState());
            verify(timelineRepository).save(any());
            verify(sseEmitterService).sendIncidentUpdate(any());
        }

        @Test
        @DisplayName("CLOSED requires RCA")
        void closedRequiresRca() {
            Incident incident = createIncident(IncidentState.RESOLVED);
            when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
            when(rcaRepository.existsByIncidentId(incident.getId())).thenReturn(false);

            StateTransitionRequest request = StateTransitionRequest.builder()
                    .targetState("CLOSED").changedBy("operator").build();

            assertThrows(RcaRequiredException.class,
                    () -> incidentService.transitionState(incident.getId(), request));
        }

        @Test
        @DisplayName("CLOSED succeeds with RCA present")
        void closedWithRca() {
            Incident incident = createIncident(IncidentState.RESOLVED);
            when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
            when(rcaRepository.existsByIncidentId(incident.getId())).thenReturn(true);
            when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StateTransitionRequest request = StateTransitionRequest.builder()
                    .targetState("CLOSED").changedBy("operator").build();

            IncidentResponse response = incidentService.transitionState(incident.getId(), request);

            assertEquals(IncidentState.CLOSED, response.getState());
        }

        @Test
        @DisplayName("Invalid transition is rejected")
        void invalidTransition() {
            Incident incident = createIncident(IncidentState.OPEN);
            when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

            StateTransitionRequest request = StateTransitionRequest.builder()
                    .targetState("CLOSED").changedBy("operator").build();

            assertThrows(RcaRequiredException.class,
                    () -> incidentService.transitionState(incident.getId(), request));
        }
    }
}
