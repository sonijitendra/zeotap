package com.ims.service;

import com.ims.dto.request.StateTransitionRequest;
import com.ims.dto.response.IncidentResponse;
import com.ims.dto.response.TimelineResponse;
import com.ims.exception.RcaRequiredException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.model.enums.IncidentState;
import com.ims.model.enums.Severity;
import com.ims.model.postgres.Incident;
import com.ims.model.postgres.IncidentTimeline;
import com.ims.repository.postgres.IncidentRepository;
import com.ims.repository.postgres.RcaRepository;
import com.ims.repository.postgres.TimelineRepository;
import com.ims.workflow.state.IncidentStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final TimelineRepository timelineRepository;
    private final RcaRepository rcaRepository;
    private final IncidentStateMachine stateMachine;
    private final SseEmitterService sseEmitterService;
    private final AuditService auditService;

    public IncidentService(IncidentRepository incidentRepository, TimelineRepository timelineRepository,
                           RcaRepository rcaRepository, IncidentStateMachine stateMachine,
                           SseEmitterService sseEmitterService, AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.timelineRepository = timelineRepository;
        this.rcaRepository = rcaRepository;
        this.stateMachine = stateMachine;
        this.sseEmitterService = sseEmitterService;
        this.auditService = auditService;
    }

    @Cacheable(value = "incidents", key = "#id")
    public IncidentResponse getIncident(UUID id) {
        Incident i = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id.toString()));
        return toResponse(i);
    }

    public Page<IncidentResponse> listIncidents(String state, String severity, String componentId, Pageable pageable) {
        IncidentState sf = state != null ? IncidentState.valueOf(state) : null;
        Severity sv = severity != null ? Severity.valueOf(severity) : null;
        String cf = componentId != null && !componentId.isBlank() ? componentId : null;
        return incidentRepository.findWithFilters(sf, sv, cf, pageable).map(this::toResponse);
    }

    @Cacheable(value = "dashboard", key = "'active-incidents'")
    public List<IncidentResponse> getActiveIncidents() {
        return incidentRepository.findActiveIncidentsSorted().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"incidents", "dashboard"}, allEntries = true)
    public IncidentResponse transitionState(UUID incidentId, StateTransitionRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", incidentId.toString()));
        IncidentState targetState = IncidentState.valueOf(request.getTargetState());
        IncidentState fromState = incident.getState();

        if (targetState == IncidentState.CLOSED && !rcaRepository.existsByIncidentId(incidentId)) {
            throw new RcaRequiredException(incidentId.toString());
        }

        stateMachine.transition(incident, targetState);

        IncidentTimeline timeline = IncidentTimeline.builder()
                .incidentId(incidentId).fromState(fromState.name()).toState(targetState.name())
                .changedBy(request.getChangedBy()).notes(request.getNotes()).build();
        timelineRepository.save(timeline);

        incident = incidentRepository.save(incident);
        sseEmitterService.sendIncidentUpdate(incident);
        auditService.logAsync("STATE_TRANSITION", "Incident", incidentId.toString(),
                java.util.Map.of("from", fromState.name(), "to", targetState.name()));
        return toResponse(incident);
    }

    public List<TimelineResponse> getTimeline(UUID incidentId) {
        if (!incidentRepository.existsById(incidentId))
            throw new ResourceNotFoundException("Incident", incidentId.toString());
        return timelineRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId).stream()
                .map(t -> TimelineResponse.builder().id(t.getId()).incidentId(t.getIncidentId())
                        .fromState(t.getFromState()).toState(t.getToState()).changedBy(t.getChangedBy())
                        .notes(t.getNotes()).createdAt(t.getCreatedAt()).build())
                .collect(Collectors.toList());
    }

    private IncidentResponse toResponse(Incident i) {
        return IncidentResponse.builder().id(i.getId()).componentId(i.getComponentId()).severity(i.getSeverity())
                .state(i.getState()).title(i.getTitle()).description(i.getDescription())
                .signalCount(i.getSignalCount()).firstSignalAt(i.getFirstSignalAt())
                .resolvedAt(i.getResolvedAt()).closedAt(i.getClosedAt()).mttrSeconds(i.getMttrSeconds())
                .assignedTo(i.getAssignedTo()).createdAt(i.getCreatedAt()).updatedAt(i.getUpdatedAt())
                .hasRca(rcaRepository.existsByIncidentId(i.getId())).build();
    }
}
