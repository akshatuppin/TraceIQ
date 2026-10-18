package com.traceiq.core.controller;

import com.traceiq.core.model.dto.TelemetryBatchRequest;
import com.traceiq.core.model.dto.TelemetryEventDto;
import com.traceiq.core.model.entity.TelemetryEvent;
import com.traceiq.core.service.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
@Tag(name = "Telemetry Ingestion & Queries", description = "High-throughput asynchronous telemetry collection and trace correlation")
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping("/events")
    @Operation(summary = "Asynchronously ingest a batch of telemetry events (Non-blocking)")
    public ResponseEntity<Map<String, Object>> ingestBatch(@Valid @RequestBody TelemetryBatchRequest request) {
        telemetryService.processBatchAsync(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "ACCEPTED",
                "message", "Telemetry batch queued for async persistence",
                "count", request.getEvents().size()
        ));
    }

    @PostMapping("/event")
    @Operation(summary = "Asynchronously ingest a single telemetry event (Non-blocking)")
    public ResponseEntity<Map<String, Object>> ingestSingle(@Valid @RequestBody TelemetryEventDto dto) {
        telemetryService.processSingleEventAsync(dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "ACCEPTED",
                "message", "Telemetry event queued for async persistence"
        ));
    }

    @GetMapping("/events")
    @Operation(summary = "Query raw telemetry events with pagination")
    public ResponseEntity<Page<TelemetryEvent>> getEvents(
            @RequestParam(defaultValue = "1") Long applicationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<TelemetryEvent> events = telemetryService.getEvents(
                applicationId,
                PageRequest.of(page, size, Sort.by("timestamp").descending())
        );
        return ResponseEntity.ok(events);
    }

    @GetMapping("/traces/{traceId}")
    @Operation(summary = "Retrieve all telemetry events belonging to a specific trace correlation ID")
    public ResponseEntity<List<TelemetryEvent>> getTraceEvents(@PathVariable String traceId) {
        List<TelemetryEvent> events = telemetryService.getEventsByTraceId(traceId);
        return ResponseEntity.ok(events);
    }
}
