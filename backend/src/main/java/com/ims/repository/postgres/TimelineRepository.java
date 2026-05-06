package com.ims.repository.postgres;

import com.ims.model.postgres.IncidentTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimelineRepository extends JpaRepository<IncidentTimeline, UUID> {

    List<IncidentTimeline> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
