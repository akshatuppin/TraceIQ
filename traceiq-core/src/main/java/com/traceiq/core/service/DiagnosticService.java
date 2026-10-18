package com.traceiq.core.service;

import com.traceiq.core.model.dto.EndpointHealthDto;
import com.traceiq.core.model.dto.MetricSeriesDto;
import com.traceiq.core.model.entity.Incident;
import com.traceiq.core.model.entity.IncidentStatus;
import com.traceiq.core.model.entity.MetricWindow;
import com.traceiq.core.repository.IncidentRepository;
import com.traceiq.core.repository.MetricWindowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiagnosticService {

    private final MetricWindowRepository metricWindowRepository;
    private final IncidentRepository incidentRepository;

    public List<EndpointHealthDto> getEndpointHealthCards(Long applicationId) {
        List<String> endpoints = metricWindowRepository.findDistinctEndpoints(applicationId);
        List<EndpointHealthDto> healthCards = new ArrayList<>();

        for (String endpoint : endpoints) {
            Optional<MetricWindow> latestOpt = metricWindowRepository.findLatestByEndpoint(applicationId, endpoint);

            if (latestOpt.isPresent()) {
                MetricWindow latest = latestOpt.get();

                String status = "HEALTHY";
                if (latest.getErrorRatePercent() > 25.0 || latest.getStatus5xxCount() >= 5) {
                    status = "CRITICAL";
                } else if (latest.getErrorRatePercent() > 5.0 || latest.getP95Ms() > 1500.0) {
                    status = "DEGRADED";
                }

                long activeIncidents = incidentRepository.countActiveIncidents(applicationId);

                healthCards.add(EndpointHealthDto.builder()
                        .endpoint(endpoint)
                        .totalRequests(latest.getRequestCount())
                        .errorRatePercent(latest.getErrorRatePercent())
                        .avgLatencyMs(latest.getAvgLatencyMs())
                        .p95LatencyMs(latest.getP95Ms())
                        .status5xxCount(latest.getStatus5xxCount())
                        .healthStatus(status)
                        .activeIncidentsCount(activeIncidents)
                        .build());
            }
        }

        return healthCards;
    }

    public List<MetricSeriesDto> getMetricSeries(Long applicationId, String endpoint, int hours) {
        LocalDateTime after = LocalDateTime.now().minusHours(hours > 0 ? hours : 1);

        List<MetricWindow> windows;
        if (endpoint != null && !endpoint.isBlank()) {
            windows = metricWindowRepository.findByApplicationIdAndEndpointAndWindowStartAfterOrderByWindowStartAsc(
                    applicationId, endpoint, after);
        } else {
            windows = metricWindowRepository.findByApplicationIdAndWindowStartAfterOrderByWindowStartAsc(
                    applicationId, after);
        }

        return windows.stream()
                .map(w -> MetricSeriesDto.builder()
                        .timestamp(w.getWindowStart())
                        .endpoint(w.getEndpoint())
                        .requestCount(w.getRequestCount())
                        .errorRatePercent(w.getErrorRatePercent())
                        .avgLatencyMs(w.getAvgLatencyMs())
                        .p95LatencyMs(w.getP95Ms())
                        .status5xxCount(w.getStatus5xxCount())
                        .build())
                .collect(Collectors.toList());
    }
}
