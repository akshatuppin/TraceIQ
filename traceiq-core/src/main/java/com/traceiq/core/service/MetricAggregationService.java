package com.traceiq.core.service;

import com.traceiq.core.model.entity.*;
import com.traceiq.core.repository.MetricWindowRepository;
import com.traceiq.core.repository.MonitoredApplicationRepository;
import com.traceiq.core.repository.TelemetryEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricAggregationService {

    private final TelemetryEventRepository telemetryEventRepository;
    private final MetricWindowRepository metricWindowRepository;
    private final MonitoredApplicationRepository applicationRepository;
    private final IncidentService incidentService;
    private final MlAnomalyClientService mlAnomalyClientService;

    @Value("${traceiq.rules.error-rate-threshold-percent:10.0}")
    private double errorRateThreshold;

    @Value("${traceiq.rules.latency-p95-threshold-ms:2000}")
    private double latencyP95Threshold;

    @Value("${traceiq.rules.consecutive-5xx-threshold:5}")
    private long consecutive5xxThreshold;

    // Run every 60 seconds
    @Scheduled(fixedRate = 60000, initialDelay = 15000)
    @Transactional
    public void aggregateMetricsScheduled() {
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusMinutes(1);

        List<MonitoredApplication> applications = applicationRepository.findAll();
        for (MonitoredApplication app : applications) {
            aggregateForApplication(app.getId(), windowStart, windowEnd);
        }
    }

    public void aggregateForApplication(Long applicationId, LocalDateTime windowStart, LocalDateTime windowEnd) {
        List<String> endpoints = telemetryEventRepository.findDistinctEndpointsByApplicationId(applicationId);

        for (String endpoint : endpoints) {
            List<TelemetryEvent> events = telemetryEventRepository
                    .findByApplicationIdAndEndpointAndTimestampBetween(applicationId, endpoint, windowStart, windowEnd);

            if (events.isEmpty()) {
                continue;
            }

            long totalRequests = events.size();
            long errorCount = events.stream().filter(e -> e.getStatusCode() >= 400).count();
            long status5xxCount = events.stream().filter(e -> e.getStatusCode() >= 500).count();
            long timeoutCount = events.stream().filter(e -> e.getStatusCode() == 504 || e.getDurationMs() >= 5000).count();

            double errorRatePercent = (totalRequests > 0) ? ((double) errorCount / totalRequests) * 100.0 : 0.0;
            double avgLatency = events.stream().mapToLong(TelemetryEvent::getDurationMs).average().orElse(0.0);

            // Compute percentiles
            List<Long> durations = events.stream()
                    .map(TelemetryEvent::getDurationMs)
                    .sorted()
                    .collect(Collectors.toList());

            double p50 = computePercentile(durations, 50);
            double p95 = computePercentile(durations, 95);
            double p99 = computePercentile(durations, 99);

            MetricWindow window = MetricWindow.builder()
                    .applicationId(applicationId)
                    .endpoint(endpoint)
                    .windowStart(windowStart)
                    .windowEnd(windowEnd)
                    .requestCount(totalRequests)
                    .errorCount(errorCount)
                    .errorRatePercent(errorRatePercent)
                    .avgLatencyMs(avgLatency)
                    .p50Ms(p50)
                    .p95Ms(p95)
                    .p99Ms(p99)
                    .status5xxCount(status5xxCount)
                    .timeoutCount(timeoutCount)
                    .build();

            MetricWindow savedWindow = metricWindowRepository.save(window);
            log.debug("Saved 1-min MetricWindow for endpoint {}: {} reqs, errorRate: {:.2f}%, p95: {:.0f}ms",
                    endpoint, totalRequests, errorRatePercent, p95);

            // 1. Evaluate Rule-Based Anomaly Detection
            evaluateRuleThresholds(savedWindow);

            // 2. Evaluate ML-Based Anomaly Detection (Isolation Forest)
            evaluateMlAnomaly(savedWindow);
        }
    }

    private void evaluateRuleThresholds(MetricWindow window) {
        // Rule A: High Error Rate Breach
        if (window.getErrorRatePercent() >= errorRateThreshold && window.getRequestCount() >= 5) {
            IncidentSeverity severity = window.getErrorRatePercent() > 30.0 ? IncidentSeverity.CRITICAL : IncidentSeverity.HIGH;
            incidentService.evaluateAndCreateIncident(
                    window.getApplicationId(),
                    window.getEndpoint(),
                    "RULE_ERROR_RATE_BREACH",
                    String.format("Elevated Error Rate (%.1f%%) on %s", window.getErrorRatePercent(), window.getEndpoint()),
                    String.format("Error rate exceeded configured SLA threshold of %.1f%% with %d total failures.",
                            errorRateThreshold, window.getErrorCount()),
                    severity,
                    window
            );
        }

        // Rule B: P95 Latency Degradation
        if (window.getP95Ms() >= latencyP95Threshold && window.getRequestCount() >= 5) {
            IncidentSeverity severity = window.getP95Ms() > 4000.0 ? IncidentSeverity.HIGH : IncidentSeverity.MEDIUM;
            incidentService.evaluateAndCreateIncident(
                    window.getApplicationId(),
                    window.getEndpoint(),
                    "RULE_LATENCY_DEGRADATION",
                    String.format("High P95 Response Latency (%.0fms) on %s", window.getP95Ms(), window.getEndpoint()),
                    String.format("P95 latency breached threshold of %.0fms. Average response time: %.0fms.",
                            latencyP95Threshold, window.getAvgLatencyMs()),
                    severity,
                    window
            );
        }

        // Rule C: Consecutive 5xx Server Crashes
        if (window.getStatus5xxCount() >= consecutive5xxThreshold) {
            incidentService.evaluateAndCreateIncident(
                    window.getApplicationId(),
                    window.getEndpoint(),
                    "RULE_SERVER_ERROR_BURST",
                    String.format("HTTP 5xx Server Error Burst (%d errors) on %s", window.getStatus5xxCount(), window.getEndpoint()),
                    String.format("Multiple 5xx internal server errors detected within 1-minute time window."),
                    IncidentSeverity.CRITICAL,
                    window
            );
        }
    }

    private void evaluateMlAnomaly(MetricWindow window) {
        AnomalyResult anomalyResult = mlAnomalyClientService.evaluateWindow(window);
        if (anomalyResult != null && anomalyResult.isAnomaly()) {
            incidentService.evaluateAndCreateIncident(
                    window.getApplicationId(),
                    window.getEndpoint(),
                    "ML_ISOLATION_FOREST_ANOMALY",
                    String.format("ML Anomaly Detected on %s (Score: %.3f)", window.getEndpoint(), anomalyResult.getAnomalyScore()),
                    String.format("Isolation Forest flagged multi-dimensional metric pattern as abnormal. %s",
                            anomalyResult.getExplanation() != null ? anomalyResult.getExplanation() : ""),
                    IncidentSeverity.MEDIUM,
                    window
            );
        }
    }

    private double computePercentile(List<Long> sortedValues, double percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }
}
