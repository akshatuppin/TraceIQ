package com.traceiq.core.repository;

import com.traceiq.core.model.entity.MetricWindow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetricWindowRepository extends JpaRepository<MetricWindow, Long> {

    List<MetricWindow> findByApplicationIdAndWindowStartAfterOrderByWindowStartAsc(
            Long applicationId, LocalDateTime after);

    List<MetricWindow> findByApplicationIdAndEndpointAndWindowStartAfterOrderByWindowStartAsc(
            Long applicationId, String endpoint, LocalDateTime after);

    Page<MetricWindow> findByApplicationIdOrderByWindowStartDesc(Long applicationId, Pageable pageable);

    @Query("SELECT m FROM MetricWindow m WHERE m.applicationId = :appId AND m.endpoint = :endpoint ORDER BY m.windowStart DESC LIMIT 1")
    Optional<MetricWindow> findLatestByEndpoint(
            @Param("appId") Long appId,
            @Param("endpoint") String endpoint);

    @Query("SELECT DISTINCT m.endpoint FROM MetricWindow m WHERE m.applicationId = :appId")
    List<String> findDistinctEndpoints(@Param("appId") Long appId);
}
