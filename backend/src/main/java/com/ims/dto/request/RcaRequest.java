package com.ims.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/**
 * RCA submission payload — all fields mandatory for incident closure.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RcaRequest {

    @NotNull(message = "incidentStartTime is required")
    private Instant incidentStartTime;

    @NotNull(message = "incidentEndTime is required")
    private Instant incidentEndTime;

    @NotBlank(message = "rootCauseCategory is required")
    private String rootCauseCategory;

    @NotBlank(message = "rootCauseDetail is required")
    private String rootCauseDetail;

    @NotBlank(message = "fixApplied is required")
    private String fixApplied;

    @NotBlank(message = "preventionSteps is required")
    private String preventionSteps;

    private String submittedBy;
}
