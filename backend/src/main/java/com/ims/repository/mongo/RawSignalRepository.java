package com.ims.repository.mongo;

import com.ims.model.mongo.RawSignal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RawSignalRepository extends MongoRepository<RawSignal, String> {

    Optional<RawSignal> findBySignalId(String signalId);

    boolean existsBySignalId(String signalId);

    List<RawSignal> findByComponentIdOrderByTimestampDesc(String componentId);

    List<RawSignal> findByIncidentIdOrderByTimestampDesc(String incidentId);

    Page<RawSignal> findByComponentId(String componentId, Pageable pageable);

    long countByTimestampAfter(Instant since);

    Page<RawSignal> findByComponentIdAndTimestampBetween(
            String componentId, Instant start, Instant end, Pageable pageable);
}
