package com.traceiq.core.repository;

import com.traceiq.core.model.entity.Incident;
import com.traceiq.core.model.entity.IncidentSeverity;
import com.traceiq.core.model.entity.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findFirstByIncidentKeyAndStatusInOrderByOpenedAtDesc(
            String incidentKey, Collection<IncidentStatus> statuses);

    Page<Incident> findByApplicationId(Long applicationId, Pageable pageable);

    Page<Incident> findByApplicationIdAndStatus(Long applicationId, IncidentStatus status, Pageable pageable);

    Page<Incident> findByApplicationIdAndSeverity(Long applicationId, IncidentSeverity severity, Pageable pageable);

    Page<Incident> findByApplicationIdAndStatusAndSeverity(
            Long applicationId, IncidentStatus status, IncidentSeverity severity, Pageable pageable);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.applicationId = :appId AND i.status != 'RESOLVED'")
    long countActiveIncidents(@Param("appId") Long appId);

    List<Incident> findByApplicationIdAndOpenedAtAfterOrderByOpenedAtDesc(
            Long applicationId, LocalDateTime after);
}
