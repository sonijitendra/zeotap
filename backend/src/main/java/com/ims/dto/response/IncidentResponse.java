package com.ims.dto.response;

import com.ims.model.enums.IncidentState;
import com.ims.model.enums.Severity;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Incident response DTO — clean separation from JPA entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentResponse {

    private UUID id;
    private String componentId;
    private Severity severity;
    private IncidentState state;
    private String title;
    private String description;
    private Integer signalCount;
    private Instant firstSignalAt;
    private Instant resolvedAt;
    private Instant closedAt;
    private Long mttrSeconds;
    private String assignedTo;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean hasRca;
}
