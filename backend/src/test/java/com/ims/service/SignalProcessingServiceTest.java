package com.ims.service;

import com.ims.dto.request.SignalRequest;
import com.ims.model.enums.IncidentState;
import com.ims.model.enums.Severity;
import com.ims.model.mongo.RawSignal;
import com.ims.model.postgres.Incident;
import com.ims.repository.mongo.RawSignalRepository;
import com.ims.repository.postgres.IncidentRepository;
import com.ims.repository.postgres.IncidentSignalRepository;
import com.ims.workflow.strategy.AlertStrategyFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Signal Processing Service Tests")
class SignalProcessingServiceTest {

    @Mock private RawSignalRepository rawSignalRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentSignalRepository incidentSignalRepository;
    @Mock private DebounceService debounceService;
    @Mock private AlertStrategyFactory alertStrategyFactory;
    @Mock private SseEmitterService sseEmitterService;
    @Mock private AuditService auditService;

    private SignalProcessingService service;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new SignalProcessingService(
                rawSignalRepository, incidentRepository, incidentSignalRepository,
                debounceService, alertStrategyFactory, sseEmitterService,
                auditService, meterRegistry);
    }

    private SignalRequest createSignal() {
        return SignalRequest.builder()
                .signalId("sig-" + UUID.randomUUID())
                .componentId("CACHE_CLUSTER_01")
                .severity("P1")
                .timestamp(Instant.now())
                .message("Redis latency spike")
                .metadata(Map.of("host", "cache-1", "region", "ap-south-1"))
                .build();
    }

    @Test
    @DisplayName("Duplicate signal is skipped")
    void duplicateSignalSkipped() {
        SignalRequest signal = createSignal();
        when(rawSignalRepository.existsBySignalId(signal.getSignalId())).thenReturn(true);

        service.processSignal(signal);

        verify(rawSignalRepository, never()).save(any(RawSignal.class));
        verify(debounceService, never()).debounce(any());
    }

    @Test
    @DisplayName("New signal creates incident when debounce returns new")
    void newSignalCreatesIncident() {
        SignalRequest signal = createSignal();
        UUID incidentId = UUID.randomUUID();

        when(rawSignalRepository.existsBySignalId(signal.getSignalId())).thenReturn(false);
        when(debounceService.debounce(signal))
                .thenReturn(DebounceService.DebounceResult.newIncident(incidentId));

        Incident createdIncident = Incident.builder()
                .id(incidentId).componentId(signal.getComponentId())
                .severity(Severity.P1).state(IncidentState.OPEN)
                .title("Test").signalCount(1)
                .firstSignalAt(signal.getTimestamp()).version(0).build();

        when(incidentRepository.save(any())).thenReturn(createdIncident);

        service.processSignal(signal);

        verify(rawSignalRepository, times(2)).save(any(RawSignal.class));
        verify(incidentRepository).save(any(Incident.class));
        verify(alertStrategyFactory).executeAlert(any(Incident.class));
        verify(sseEmitterService).sendIncidentUpdate(any(Incident.class));
        verify(debounceService).updateIncidentId(signal.getComponentId(), incidentId);
    }

    @Test
    @DisplayName("Signal links to existing incident on debounce hit")
    void signalLinksToExistingIncident() {
        SignalRequest signal = createSignal();
        UUID incidentId = UUID.randomUUID();

        when(rawSignalRepository.existsBySignalId(signal.getSignalId())).thenReturn(false);
        when(debounceService.debounce(signal))
                .thenReturn(DebounceService.DebounceResult.existing(incidentId));

        Incident existingIncident = Incident.builder()
                .id(incidentId).componentId(signal.getComponentId())
                .severity(Severity.P2).state(IncidentState.OPEN)
                .title("Existing").signalCount(5)
                .firstSignalAt(Instant.now().minusSeconds(30)).version(0).build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(existingIncident));
        when(incidentSignalRepository.existsBySignalId(signal.getSignalId())).thenReturn(false);

        service.processSignal(signal);

        verify(incidentRepository, never()).save(argThat(i ->
                i.getId() == null)); // shouldn't create new
        verify(alertStrategyFactory, never()).executeAlert(any());
    }

    @Test
    @DisplayName("Signal severity escalation on existing incident")
    void signalSeverityEscalation() {
        SignalRequest signal = createSignal();
        signal.setSeverity("P0"); // higher severity than existing
        UUID incidentId = UUID.randomUUID();

        when(rawSignalRepository.existsBySignalId(signal.getSignalId())).thenReturn(false);
        when(debounceService.debounce(signal))
                .thenReturn(DebounceService.DebounceResult.existing(incidentId));

        Incident existingIncident = Incident.builder()
                .id(incidentId).componentId(signal.getComponentId())
                .severity(Severity.P2).state(IncidentState.OPEN)
                .title("Existing").signalCount(5)
                .firstSignalAt(Instant.now().minusSeconds(30)).version(0).build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(existingIncident));
        when(incidentSignalRepository.existsBySignalId(signal.getSignalId())).thenReturn(false);

        service.processSignal(signal);

        verify(incidentRepository).save(argThat(i -> i.getSeverity() == Severity.P0));
    }
}
