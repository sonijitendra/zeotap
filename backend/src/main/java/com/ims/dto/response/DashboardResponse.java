package com.ims.dto.response;

import lombok.*;

import java.util.Map;

/**
 * Dashboard summary DTO with aggregated metrics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private long totalActiveIncidents;
    private long totalSignalsToday;
    private Map<String, Long> incidentsBySeverity;
    private Map<String, Long> incidentsByState;
    private double avgMttrSeconds;
    private long signalsPerSecond;
}
