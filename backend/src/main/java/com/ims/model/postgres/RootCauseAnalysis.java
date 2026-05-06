package com.ims.model.postgres;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Root Cause Analysis entity — mandatory before incident closure.
 * One-to-one relationship with Incident.
 */
@Entity
@Table(name = "root_cause_analyses")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RootCauseAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false, unique = true)
    private UUID incidentId;

    @Column(name = "incident_start_time", nullable = false)
    private Instant incidentStartTime;

    @Column(name = "incident_end_time", nullable = false)
    private Instant incidentEndTime;

    @Column(name = "root_cause_category", nullable = false, length = 100)
    private String rootCauseCategory;

    @Column(name = "root_cause_detail", nullable = false, columnDefinition = "TEXT")
    private String rootCauseDetail;

    @Column(name = "fix_applied", nullable = false, columnDefinition = "TEXT")
    private String fixApplied;

    @Column(name = "prevention_steps", nullable = false, columnDefinition = "TEXT")
    private String preventionSteps;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private Instant submittedAt = Instant.now();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
