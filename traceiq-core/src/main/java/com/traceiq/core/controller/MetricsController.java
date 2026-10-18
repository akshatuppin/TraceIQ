package com.traceiq.core.controller;

import com.traceiq.core.model.dto.EndpointHealthDto;
import com.traceiq.core.model.dto.MetricSeriesDto;
import com.traceiq.core.service.DiagnosticService;
import com.traceiq.core.service.MetricAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics & Diagnostic Queries", description = "Query endpoint health statuses and time-series performance trends")
public class MetricsController {

    private final DiagnosticService diagnosticService;
    private final MetricAggregationService metricAggregationService;

    @GetMapping("/endpoints")
    @Operation(summary = "Get aggregated health summary cards for all endpoints in an application")
    public ResponseEntity<List<EndpointHealthDto>> getEndpointHealthCards(
            @RequestParam(defaultValue = "1") Long applicationId
    ) {
        List<EndpointHealthDto> cards = diagnosticService.getEndpointHealthCards(applicationId);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/timeseries")
    @Operation(summary = "Fetch 1-minute rolling metric series for interactive dashboard charts")
    public ResponseEntity<List<MetricSeriesDto>> getMetricSeries(
            @RequestParam(defaultValue = "1") Long applicationId,
            @RequestParam(required = false) String endpoint,
            @RequestParam(defaultValue = "1") int hours
    ) {
        List<MetricSeriesDto> series = diagnosticService.getMetricSeries(applicationId, endpoint, hours);
        return ResponseEntity.ok(series);
    }

    @PostMapping("/aggregate")
    @Operation(summary = "Manually trigger on-demand metric aggregation and incident detection")
    public ResponseEntity<Map<String, String>> triggerManualAggregation(
            @RequestParam(defaultValue = "1") Long applicationId,
            @RequestParam(defaultValue = "5") int minutesBack
    ) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMinutes(minutesBack);
        metricAggregationService.aggregateForApplication(applicationId, start, end);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", String.format("Aggregation executed for application %d between %s and %s", applicationId, start, end)
        ));
    }
}
