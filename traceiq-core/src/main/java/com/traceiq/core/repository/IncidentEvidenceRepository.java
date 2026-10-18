package com.traceiq.core.repository;

import com.traceiq.core.model.entity.IncidentEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentEvidenceRepository extends JpaRepository<IncidentEvidence, Long> {
    List<IncidentEvidence> findByIncidentIdOrderByOccurredAtDesc(Long incidentId);
}
