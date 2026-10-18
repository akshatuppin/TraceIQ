package com.traceiq.core.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "telemetry_events",
    indexes = {
        @Index(name = "idx_telemetry_app_ts", columnList = "application_id, timestamp"),
        @Index(name = "idx_telemetry_endpoint_ts", columnList = "endpoint, timestamp"),
        @Index(name = "idx_telemetry_trace_id", columnList = "trace_id"),
        @Index(name = "idx_telemetry_status", columnList = "status_code")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "exception_type", length = 255)
    private String exceptionType;

    @Column(name = "stack_trace_snippet", columnDefinition = "TEXT")
    private String stackTraceSnippet;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
