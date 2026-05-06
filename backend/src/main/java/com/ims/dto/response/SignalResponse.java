package com.ims.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Signal response DTO for API consumers.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalResponse {

    private String signalId;
    private String componentId;
    private String severity;
    private Instant timestamp;
    private String message;
    private Map<String, Object> metadata;
    private String incidentId;
    private boolean processed;
    private Instant processedAt;
}
