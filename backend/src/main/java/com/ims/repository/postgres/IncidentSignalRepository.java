package com.ims.repository.postgres;

import com.ims.model.postgres.IncidentSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentSignalRepository extends JpaRepository<IncidentSignal, UUID> {

    List<IncidentSignal> findByIncidentId(UUID incidentId);

    boolean existsBySignalId(String signalId);
}
