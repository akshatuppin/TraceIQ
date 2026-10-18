package com.traceiq.core.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "metric_windows",
    indexes = {
        @Index(name = "idx_metric_app_endp_window", columnList = "application_id, endpoint, window_start"),
        @Index(name = "idx_metric_window_start", columnList = "window_start")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Column(name = "request_count", nullable = false)
    private long requestCount;

    @Column(name = "error_count", nullable = false)
    private long errorCount;

    @Column(name = "error_rate_percent", nullable = false)
    private double errorRatePercent;

    @Column(name = "avg_latency_ms", nullable = false)
    private double avgLatencyMs;

    @Column(name = "p50_ms", nullable = false)
    private double p50Ms;

    @Column(name = "p95_ms", nullable = false)
    private double p95Ms;

    @Column(name = "p99_ms", nullable = false)
    private double p99Ms;

    @Column(name = "status_5xx_count", nullable = false)
    private long status5xxCount;

    @Column(name = "timeout_count", nullable = false)
    private long timeoutCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
