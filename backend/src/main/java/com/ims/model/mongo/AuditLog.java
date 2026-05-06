package com.ims.model.mongo;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Audit log document stored in MongoDB for compliance and debugging.
 * Captures every significant system action with full payload.
 */
@Document(collection = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String action;

    @Indexed
    private String entityType;

    @Indexed
    private String entityId;

    private Map<String, Object> payload;

    private String actor;

    @CreatedDate
    @Indexed
    private Instant timestamp;
}
