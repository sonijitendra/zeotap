package com.ims.model.postgres;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Junction table linking raw signal IDs to their parent incident.
 */
@Entity
@Table(name = "incident_signals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "signal_id", nullable = false)
    private String signalId;

    @Column(name = "linked_at", nullable = false)
    @Builder.Default
    private Instant linkedAt = Instant.now();
}
