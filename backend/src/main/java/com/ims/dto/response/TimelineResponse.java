package com.ims.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Timeline entry response DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineResponse {

    private UUID id;
    private UUID incidentId;
    private String fromState;
    private String toState;
    private String changedBy;
    private String notes;
    private Instant createdAt;
}
