package com.ims.service;

import com.ims.dto.request.RcaRequest;
import com.ims.dto.response.RcaResponse;
import com.ims.exception.ResourceNotFoundException;
import com.ims.model.enums.IncidentState;
import com.ims.model.enums.Severity;
import com.ims.model.postgres.Incident;
import com.ims.model.postgres.RootCauseAnalysis;
import com.ims.repository.postgres.IncidentRepository;
import com.ims.repository.postgres.RcaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RCA Service Tests")
class RcaServiceTest {

    @Mock private RcaRepository rcaRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private AuditService auditService;

    @InjectMocks private RcaService rcaService;

    private UUID incidentId;
    private Incident incident;

    @BeforeEach
    void setUp() {
        incidentId = UUID.randomUUID();
        incident = Incident.builder()
                .id(incidentId).componentId("TEST").severity(Severity.P1)
                .state(IncidentState.RESOLVED).title("Test")
                .firstSignalAt(Instant.now().minusSeconds(600)).signalCount(5).build();
    }

    @Test
    @DisplayName("Submit RCA calculates MTTR")
    void submitRcaCalculatesMttr() {
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(rcaRepository.existsByIncidentId(incidentId)).thenReturn(false);
        when(rcaRepository.save(any())).thenAnswer(inv -> {
            RootCauseAnalysis rca = inv.getArgument(0);
            rca.setId(UUID.randomUUID());
            return rca;
        });

        RcaRequest request = RcaRequest.builder()
                .incidentStartTime(Instant.now().minusSeconds(600))
                .incidentEndTime(Instant.now())
                .rootCauseCategory("Infrastructure")
                .rootCauseDetail("Disk full on cache node")
                .fixApplied("Expanded disk, added monitoring")
                .preventionSteps("Auto-scaling policy for disk")
                .submittedBy("engineer@company.com").build();

        RcaResponse response = rcaService.submitRca(incidentId, request);

        assertNotNull(response);
        assertEquals(incidentId, response.getIncidentId());
        verify(incidentRepository).save(any());
    }

    @Test
    @DisplayName("Submit RCA for non-existent incident throws")
    void submitRcaNotFound() {
        when(incidentRepository.findById(any())).thenReturn(Optional.empty());

        RcaRequest request = RcaRequest.builder().rootCauseCategory("Test")
                .rootCauseDetail("Test").fixApplied("Test").preventionSteps("Test")
                .incidentStartTime(Instant.now()).incidentEndTime(Instant.now()).build();

        assertThrows(ResourceNotFoundException.class,
                () -> rcaService.submitRca(UUID.randomUUID(), request));
    }

    @Test
    @DisplayName("Duplicate RCA submission throws")
    void duplicateRcaThrows() {
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(rcaRepository.existsByIncidentId(incidentId)).thenReturn(true);

        RcaRequest request = RcaRequest.builder().rootCauseCategory("Test")
                .rootCauseDetail("Test").fixApplied("Test").preventionSteps("Test")
                .incidentStartTime(Instant.now()).incidentEndTime(Instant.now()).build();

        assertThrows(IllegalArgumentException.class,
                () -> rcaService.submitRca(incidentId, request));
    }
}
