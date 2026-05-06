package com.ims.repository.postgres;

import com.ims.model.postgres.RootCauseAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RcaRepository extends JpaRepository<RootCauseAnalysis, UUID> {

    Optional<RootCauseAnalysis> findByIncidentId(UUID incidentId);

    boolean existsByIncidentId(UUID incidentId);
}
