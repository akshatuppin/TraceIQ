package com.traceiq.core.repository;

import com.traceiq.core.model.entity.AnomalyResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnomalyResultRepository extends JpaRepository<AnomalyResult, Long> {
    Page<AnomalyResult> findByApplicationIdAndIsAnomalyTrueOrderByDetectedAtDesc(
            Long applicationId, Pageable pageable);

    List<AnomalyResult> findByApplicationIdAndWindowStartAfterOrderByWindowStartAsc(
            Long applicationId, LocalDateTime after);
}
