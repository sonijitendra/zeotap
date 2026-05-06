package com.ims.model.mongo;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Raw signal document stored in MongoDB for high-throughput writes
 * and flexible schema (metadata varies by signal source).
 */
@Document(collection = "raw_signals")
@CompoundIndex(name = "idx_component_timestamp", def = "{'componentId': 1, 'timestamp': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawSignal {

    @Id
    private String id;

    @Indexed(unique = true)
    private String signalId;

    @Indexed
    private String componentId;

    private String severity;

    @Indexed
    private Instant timestamp;

    private String message;

    private Map<String, Object> metadata;

    @Indexed
    private String incidentId;

    private boolean processed;

    @CreatedDate
    private Instant processedAt;
}
