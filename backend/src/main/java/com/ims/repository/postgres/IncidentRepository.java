package com.ims.repository.postgres;

import com.ims.model.enums.IncidentState;
import com.ims.model.enums.Severity;
import com.ims.model.postgres.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    /**
     * Find active (non-closed) incidents for a component — used in debounce logic.
     */
    Optional<Incident> findFirstByComponentIdAndStateNotOrderByCreatedAtDesc(
            String componentId, IncidentState excludedState);

    /**
     * All active incidents sorted by severity priority for dashboard.
     */
    @Query("SELECT i FROM Incident i WHERE i.state <> 'CLOSED' ORDER BY i.severity ASC, i.createdAt DESC")
    List<Incident> findActiveIncidentsSorted();

    /**
     * Paginated incident listing with optional filters.
     */
    @Query("SELECT i FROM Incident i WHERE " +
           "(:state IS NULL OR i.state = :state) AND " +
           "(:severity IS NULL OR i.severity = :severity) AND " +
           "(:componentId IS NULL OR i.componentId = :componentId) " +
           "ORDER BY i.createdAt DESC")
    Page<Incident> findWithFilters(
            @Param("state") IncidentState state,
            @Param("severity") Severity severity,
            @Param("componentId") String componentId,
            Pageable pageable);

    /**
     * Count active incidents by state.
     */
    long countByState(IncidentState state);

    /**
     * Count active incidents by severity.
     */
    long countBySeverity(Severity severity);

    /**
     * Count incidents not in CLOSED state.
     */
    long countByStateNot(IncidentState state);

    /**
     * Average MTTR for resolved/closed incidents.
     */
    @Query("SELECT AVG(i.mttrSeconds) FROM Incident i WHERE i.mttrSeconds IS NOT NULL")
    Double findAverageMttr();

    /**
     * Find incidents created after a timestamp.
     */
    List<Incident> findByCreatedAtAfter(Instant since);
}
