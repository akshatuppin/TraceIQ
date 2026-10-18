package com.traceiq.core.service;

import com.traceiq.core.model.dto.IncidentDto;
import com.traceiq.core.model.dto.IncidentStatusUpdateDto;
import com.traceiq.core.model.entity.*;
import com.traceiq.core.repository.IncidentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IncidentServiceTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    @DisplayName("Should create a new incident and prevent duplicates within cooldown window")
    void shouldCreateIncidentAndDeduplicate() {
        MetricWindow window = MetricWindow.builder()
                .applicationId(1L)
                .endpoint("/api/orders")
                .windowStart(LocalDateTime.now().minusMinutes(1))
                .windowEnd(LocalDateTime.now())
                .requestCount(50)
                .errorCount(20)
                .errorRatePercent(40.0)
                .avgLatencyMs(850.0)
                .p95Ms(2500.0)
                .status5xxCount(10)
                .build();

        // 1. First trigger -> should create new Incident
        Optional<Incident> firstCreated = incidentService.evaluateAndCreateIncident(
                1L,
                "/api/orders",
                "RULE_ERROR_RATE_BREACH",
                "Elevated Error Rate on /api/orders",
                "40% error rate detected",
                IncidentSeverity.CRITICAL,
                window
        );

        assertThat(firstCreated).isPresent();
        Incident incident1 = firstCreated.get();
        assertThat(incident1.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(incident1.getSeverity()).isEqualTo(IncidentSeverity.CRITICAL);

        // 2. Second trigger immediately with same key -> should deduplicate
        Optional<Incident> secondTriggered = incidentService.evaluateAndCreateIncident(
                1L,
                "/api/orders",
                "RULE_ERROR_RATE_BREACH",
                "Elevated Error Rate on /api/orders",
                "40% error rate detected",
                IncidentSeverity.CRITICAL,
                window
        );

        assertThat(secondTriggered).isPresent();
        assertThat(secondTriggered.get().getId()).isEqualTo(incident1.getId());
    }

    @Test
    @DisplayName("Should update incident lifecycle status to ACKNOWLEDGED and RESOLVED")
    void shouldUpdateIncidentLifecycle() {
        MetricWindow window = MetricWindow.builder()
                .applicationId(1L)
                .endpoint("/api/payments")
                .windowStart(LocalDateTime.now().minusMinutes(1))
                .windowEnd(LocalDateTime.now())
                .requestCount(10)
                .errorCount(5)
                .errorRatePercent(50.0)
                .avgLatencyMs(3200.0)
                .p95Ms(4500.0)
                .status5xxCount(5)
                .build();

        Incident incident = incidentService.evaluateAndCreateIncident(
                1L,
                "/api/payments",
                "RULE_LATENCY_DEGRADATION",
                "High latency on /api/payments",
                "Payment gateway timeout",
                IncidentSeverity.HIGH,
                window
        ).orElseThrow();

        // Acknowledge incident
        IncidentStatusUpdateDto ackDto = IncidentStatusUpdateDto.builder()
                .status(IncidentStatus.ACKNOWLEDGED)
                .assignedTo("john_dev")
                .notes("Investigating database lock")
                .build();

        IncidentDto acknowledged = incidentService.updateStatus(incident.getId(), ackDto, "admin");
        assertThat(acknowledged.getStatus()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
        assertThat(acknowledged.getAssignedTo()).isEqualTo("john_dev");
        assertThat(acknowledged.getAcknowledgedAt()).isNotNull();

        // Resolve incident
        IncidentStatusUpdateDto resolveDto = IncidentStatusUpdateDto.builder()
                .status(IncidentStatus.RESOLVED)
                .notes("Restarted payment connection pool. Latency returned to 180ms.")
                .build();

        IncidentDto resolved = incidentService.updateStatus(incident.getId(), resolveDto, "admin");
        assertThat(resolved.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(resolved.getResolvedAt()).isNotNull();
    }
}
