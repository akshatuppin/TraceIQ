package com.traceiq.core.controller;

import com.traceiq.core.model.dto.IncidentDto;
import com.traceiq.core.model.dto.IncidentStatusUpdateDto;
import com.traceiq.core.model.entity.IncidentSeverity;
import com.traceiq.core.model.entity.IncidentStatus;
import com.traceiq.core.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incident Lifecycle & Triage", description = "Query, investigate evidence, and update incident resolution states")
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    @Operation(summary = "Query incidents with optional filters by status, severity, and pagination")
    public ResponseEntity<Page<IncidentDto>> getIncidents(
            @RequestParam(defaultValue = "1") Long applicationId,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Page<IncidentDto> incidents = incidentService.getIncidents(
                applicationId,
                status,
                severity,
                PageRequest.of(page, size, Sort.by("openedAt").descending())
        );
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed incident record including correlated evidence and stack traces")
    public ResponseEntity<IncidentDto> getIncidentById(@PathVariable Long id) {
        IncidentDto incident = incidentService.getIncidentById(id);
        return ResponseEntity.ok(incident);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update incident status (ACKNOWLEDGED / RESOLVED) with triage notes")
    public ResponseEntity<IncidentDto> updateIncidentStatus(
            @PathVariable Long id,
            @Valid @RequestBody IncidentStatusUpdateDto dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = userDetails != null ? userDetails.getUsername() : "ANONYMOUS";
        IncidentDto updated = incidentService.updateStatus(id, dto, username);
        return ResponseEntity.ok(updated);
    }
}
