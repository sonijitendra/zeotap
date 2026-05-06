package com.ims.service;

import com.ims.dto.request.RcaRequest;
import com.ims.dto.response.RcaResponse;
import com.ims.exception.ResourceNotFoundException;
import com.ims.model.postgres.Incident;
import com.ims.model.postgres.RootCauseAnalysis;
import com.ims.repository.postgres.IncidentRepository;
import com.ims.repository.postgres.RcaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class RcaService {

    private static final Logger log = LoggerFactory.getLogger(RcaService.class);

    private final RcaRepository rcaRepository;
    private final IncidentRepository incidentRepository;
    private final AuditService auditService;

    public RcaService(RcaRepository rcaRepository, IncidentRepository incidentRepository, AuditService auditService) {
        this.rcaRepository = rcaRepository;
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
    }

    @Transactional
    @CacheEvict(value = {"incidents", "dashboard"}, allEntries = true)
    public RcaResponse submitRca(UUID incidentId, RcaRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", incidentId.toString()));

        if (rcaRepository.existsByIncidentId(incidentId)) {
            throw new IllegalArgumentException("RCA already submitted for incident " + incidentId);
        }

        Instant submittedAt = Instant.now();

        // Calculate MTTR: RCA submission time - first signal time
        if (incident.getFirstSignalAt() != null && incident.getMttrSeconds() == null) {
            long mttr = Duration.between(incident.getFirstSignalAt(), submittedAt).getSeconds();
            incident.setMttrSeconds(mttr);
            incidentRepository.save(incident);
        }

        RootCauseAnalysis rca = RootCauseAnalysis.builder()
                .incidentId(incidentId)
                .incidentStartTime(request.getIncidentStartTime())
                .incidentEndTime(request.getIncidentEndTime())
                .rootCauseCategory(request.getRootCauseCategory())
                .rootCauseDetail(request.getRootCauseDetail())
                .fixApplied(request.getFixApplied())
                .preventionSteps(request.getPreventionSteps())
                .submittedBy(request.getSubmittedBy())
                .submittedAt(submittedAt)
                .build();

        rca = rcaRepository.save(rca);
        auditService.logAsync("RCA_SUBMITTED", "RCA", rca.getId().toString(), request);
        log.info("RCA submitted for incident {}", incidentId);
        return toResponse(rca);
    }

    public RcaResponse getRca(UUID incidentId) {
        RootCauseAnalysis rca = rcaRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("RCA", incidentId.toString()));
        return toResponse(rca);
    }

    private RcaResponse toResponse(RootCauseAnalysis rca) {
        return RcaResponse.builder()
                .id(rca.getId()).incidentId(rca.getIncidentId())
                .incidentStartTime(rca.getIncidentStartTime()).incidentEndTime(rca.getIncidentEndTime())
                .rootCauseCategory(rca.getRootCauseCategory()).rootCauseDetail(rca.getRootCauseDetail())
                .fixApplied(rca.getFixApplied()).preventionSteps(rca.getPreventionSteps())
                .submittedBy(rca.getSubmittedBy()).submittedAt(rca.getSubmittedAt()).build();
    }
}
