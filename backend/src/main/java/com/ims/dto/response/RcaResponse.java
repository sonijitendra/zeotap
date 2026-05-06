package com.ims.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * RCA response DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RcaResponse {

    private UUID id;
    private UUID incidentId;
    private Instant incidentStartTime;
    private Instant incidentEndTime;
    private String rootCauseCategory;
    private String rootCauseDetail;
    private String fixApplied;
    private String preventionSteps;
    private String submittedBy;
    private Instant submittedAt;
}
