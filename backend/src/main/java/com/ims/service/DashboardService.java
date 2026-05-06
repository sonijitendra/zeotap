package com.ims.service;

import com.ims.dto.response.DashboardResponse;
import com.ims.model.enums.IncidentState;
import com.ims.model.enums.Severity;
import com.ims.repository.mongo.RawSignalRepository;
import com.ims.repository.postgres.IncidentRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final IncidentRepository incidentRepository;
    private final RawSignalRepository rawSignalRepository;

    public DashboardService(IncidentRepository incidentRepository, RawSignalRepository rawSignalRepository) {
        this.incidentRepository = incidentRepository;
        this.rawSignalRepository = rawSignalRepository;
    }

    @Cacheable(value = "dashboard", key = "'summary'")
    public DashboardResponse getDashboardSummary() {
        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Severity s : Severity.values()) {
            bySeverity.put(s.name(), incidentRepository.countBySeverity(s));
        }

        Map<String, Long> byState = new LinkedHashMap<>();
        for (IncidentState s : IncidentState.values()) {
            byState.put(s.name(), incidentRepository.countByState(s));
        }

        Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long signalsToday = rawSignalRepository.countByTimestampAfter(todayStart);
        Double avgMttr = incidentRepository.findAverageMttr();

        return DashboardResponse.builder()
                .totalActiveIncidents(incidentRepository.countByStateNot(IncidentState.CLOSED))
                .totalSignalsToday(signalsToday)
                .incidentsBySeverity(bySeverity)
                .incidentsByState(byState)
                .avgMttrSeconds(avgMttr != null ? avgMttr : 0)
                .signalsPerSecond(0) // Updated by throughput logger
                .build();
    }
}
