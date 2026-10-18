package com.traceiq.core.service;

import com.traceiq.core.exception.ResourceNotFoundException;
import com.traceiq.core.model.dto.IncidentDto;
import com.traceiq.core.model.dto.IncidentStatusUpdateDto;
import com.traceiq.core.model.entity.*;
import com.traceiq.core.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentEvidenceRepository evidenceRepository;
    private final TelemetryEventRepository telemetryEventRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${traceiq.rules.incident-cooldown-minutes:15}")
    private int incidentCooldownMinutes;

    @Transactional
    public Optional<Incident> evaluateAndCreateIncident(
            Long applicationId,
            String endpoint,
            String triggerType,
            String title,
            String description,
            IncidentSeverity severity,
            MetricWindow window
    ) {
        String incidentKey = String.format("APP_%d_%s_%s", applicationId, endpoint.replace("/", "_"), triggerType);

        // Deduplication check: check if an active incident with this key exists within cooldown
        Optional<Incident> existingActive = incidentRepository
                .findFirstByIncidentKeyAndStatusInOrderByOpenedAtDesc(
                        incidentKey,
                        List.of(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED)
                );

        if (existingActive.isPresent()) {
            Incident incident = existingActive.get();
            LocalDateTime cooldownLimit = incident.getOpenedAt().plusMinutes(incidentCooldownMinutes);
            if (LocalDateTime.now().isBefore(cooldownLimit)) {
                log.info("Incident [{}] is within cooldown window. Appending correlated evidence rather than creating duplicate.", incidentKey);
                correlateEvidence(incident, window);
                return Optional.of(incident);
            }
        }

        // Create new incident
        Incident incident = Incident.builder()
                .incidentKey(incidentKey)
                .applicationId(applicationId)
                .endpoint(endpoint)
                .title(title)
                .description(description)
                .severity(severity)
                .status(IncidentStatus.OPEN)
                .triggeredBy(triggerType)
                .openedAt(LocalDateTime.now())
                .build();

        Incident saved = incidentRepository.save(incident);
        log.warn("🚨 NEW INCIDENT TRIGGERED: [{}] {} (Severity: {})", incidentKey, title, severity);

        // Correlate Evidence
        correlateEvidence(saved, window);

        // Record Audit Log
        auditLogRepository.save(AuditLog.builder()
                .action("INCIDENT_TRIGGERED")
                .performedBy("SYSTEM_DETECTOR")
                .entityType("INCIDENT")
                .entityId(saved.getId())
                .details(String.format("Triggered by %s. Severity: %s", triggerType, severity))
                .build());

        return Optional.of(saved);
    }

    private void correlateEvidence(Incident incident, MetricWindow window) {
        if (window == null) return;

        // Evidence 1: Metric window metrics summary
        evidenceRepository.save(IncidentEvidence.builder()
                .incidentId(incident.getId())
                .evidenceType(EvidenceType.STATUS_5XX_BURST)
                .description(String.format("Metric Window Snapshot: %d requests, %.2f%% error rate, %.0fms p95 latency, %d 5xx errors",
                        window.getRequestCount(), window.getErrorRatePercent(), window.getP95Ms(), window.getStatus5xxCount()))
                .rawPayload(String.format("{\"rpm\":%d,\"errorRate\":%.2f,\"p95\":%.0f,\"5xx\":%d}",
                        window.getRequestCount(), window.getErrorRatePercent(), window.getP95Ms(), window.getStatus5xxCount()))
                .occurredAt(window.getWindowEnd())
                .build());

        // Evidence 2: Fetch recent 5xx error stack traces
        List<TelemetryEvent> recentErrors = telemetryEventRepository.findServerErrorsByEndpointAndWindow(
                incident.getApplicationId(),
                incident.getEndpoint(),
                window.getWindowStart(),
                window.getWindowEnd()
        );

        int count = 0;
        for (TelemetryEvent err : recentErrors) {
            if (count++ >= 3) break; // Limit to top 3 representative stack traces
            evidenceRepository.save(IncidentEvidence.builder()
                    .incidentId(incident.getId())
                    .evidenceType(EvidenceType.STACK_TRACE)
                    .description(String.format("HTTP %d Exception: %s (%s)",
                            err.getStatusCode(),
                            err.getExceptionType() != null ? err.getExceptionType() : "UnknownException",
                            err.getErrorMessage() != null ? err.getErrorMessage() : "No message"))
                    .rawPayload(err.getStackTraceSnippet() != null ? err.getStackTraceSnippet() : "No stack trace available")
                    .occurredAt(err.getTimestamp())
                    .build());
        }
    }

    @Transactional
    public IncidentDto updateStatus(Long incidentId, IncidentStatusUpdateDto dto, String username) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + incidentId));

        IncidentStatus oldStatus = incident.getStatus();
        incident.setStatus(dto.getStatus());

        if (dto.getNotes() != null && !dto.getNotes().isBlank()) {
            incident.setResolutionNotes(dto.getNotes());
        }
        if (dto.getAssignedTo() != null && !dto.getAssignedTo().isBlank()) {
            incident.setAssignedTo(dto.getAssignedTo());
        }

        if (dto.getStatus() == IncidentStatus.ACKNOWLEDGED && incident.getAcknowledgedAt() == null) {
            incident.setAcknowledgedAt(LocalDateTime.now());
        } else if (dto.getStatus() == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(LocalDateTime.now());
        }

        Incident updated = incidentRepository.save(incident);

        // Record Audit Log
        auditLogRepository.save(AuditLog.builder()
                .action("INCIDENT_STATUS_CHANGE")
                .performedBy(username != null ? username : "ANONYMOUS")
                .entityType("INCIDENT")
                .entityId(incident.getId())
                .details(String.format("Status changed from %s to %s. Notes: %s", oldStatus, dto.getStatus(), dto.getNotes()))
                .build());

        return mapToDto(updated);
    }

    public Page<IncidentDto> getIncidents(Long applicationId, IncidentStatus status, IncidentSeverity severity, Pageable pageable) {
        Page<Incident> page;
        if (status != null && severity != null) {
            page = incidentRepository.findByApplicationIdAndStatusAndSeverity(applicationId, status, severity, pageable);
        } else if (status != null) {
            page = incidentRepository.findByApplicationIdAndStatus(applicationId, status, pageable);
        } else if (severity != null) {
            page = incidentRepository.findByApplicationIdAndSeverity(applicationId, severity, pageable);
        } else {
            page = incidentRepository.findByApplicationId(applicationId, pageable);
        }
        return page.map(this::mapToDto);
    }

    public IncidentDto getIncidentById(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));
        return mapToDto(incident);
    }

    private IncidentDto mapToDto(Incident incident) {
        List<IncidentEvidence> evidence = evidenceRepository.findByIncidentIdOrderByOccurredAtDesc(incident.getId());

        return IncidentDto.builder()
                .id(incident.getId())
                .incidentKey(incident.getIncidentKey())
                .applicationId(incident.getApplicationId())
                .endpoint(incident.getEndpoint())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .triggeredBy(incident.getTriggeredBy())
                .assignedTo(incident.getAssignedTo())
                .resolutionNotes(incident.getResolutionNotes())
                .openedAt(incident.getOpenedAt())
                .acknowledgedAt(incident.getAcknowledgedAt())
                .resolvedAt(incident.getResolvedAt())
                .evidence(evidence)
                .build();
    }
}
