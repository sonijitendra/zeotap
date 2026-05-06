package com.ims.model.postgres;

import com.ims.model.enums.IncidentState;
import com.ims.model.enums.Severity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Core incident/work-item entity persisted in PostgreSQL.
 * Uses optimistic locking via @Version to prevent concurrent state corruption.
 */
@Entity
@Table(name = "incidents")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "component_id", nullable = false)
    private String componentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentState state;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "signal_count", nullable = false)
    @Builder.Default
    private Integer signalCount = 1;

    @Column(name = "first_signal_at", nullable = false)
    private Instant firstSignalAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "mttr_seconds")
    private Long mttrSeconds;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Version
    @Column(nullable = false)
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
