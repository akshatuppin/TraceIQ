package com.traceiq.core.repository;

import com.traceiq.core.model.entity.TelemetryEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelemetryEventRepository extends JpaRepository<TelemetryEvent, Long> {

    List<TelemetryEvent> findByApplicationIdAndTimestampBetween(
            Long applicationId, LocalDateTime start, LocalDateTime end);

    List<TelemetryEvent> findByApplicationIdAndEndpointAndTimestampBetween(
            Long applicationId, String endpoint, LocalDateTime start, LocalDateTime end);

    Page<TelemetryEvent> findByApplicationId(Long applicationId, Pageable pageable);

    List<TelemetryEvent> findByTraceId(String traceId);

    @Query("SELECT DISTINCT e.endpoint FROM TelemetryEvent e WHERE e.applicationId = :appId")
    List<String> findDistinctEndpointsByApplicationId(@Param("appId") Long appId);

    @Query("SELECT e FROM TelemetryEvent e WHERE e.applicationId = :appId AND e.statusCode >= 400 AND e.timestamp BETWEEN :start AND :end ORDER BY e.timestamp DESC")
    List<TelemetryEvent> findErrorsByApplicationAndWindow(
            @Param("appId") Long appId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT e FROM TelemetryEvent e WHERE e.applicationId = :appId AND e.endpoint = :endpoint AND e.statusCode >= 500 AND e.timestamp BETWEEN :start AND :end ORDER BY e.timestamp DESC")
    List<TelemetryEvent> findServerErrorsByEndpointAndWindow(
            @Param("appId") Long appId,
            @Param("endpoint") String endpoint,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
