package com.traceiq.core.repository;

import com.traceiq.core.model.entity.MonitoredApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonitoredApplicationRepository extends JpaRepository<MonitoredApplication, Long> {
    Optional<MonitoredApplication> findByAppKey(String appKey);
    Optional<MonitoredApplication> findByAppName(String appName);
    boolean existsByAppName(String appName);
    boolean existsByAppKey(String appKey);
}
