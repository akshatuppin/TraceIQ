package com.traceiq.core.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "anomaly_results",
    indexes = {
        @Index(name = "idx_anomaly_app_window", columnList = "application_id, window_start"),
        @Index(name = "idx_anomaly_flag", columnList = "is_anomaly")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyResult {

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

    @Column(name = "anomaly_score", nullable = false)
    private double anomalyScore;

    @Column(name = "is_anomaly", nullable = false)
    private boolean isAnomaly;

    @Column(name = "feature_vector_json", columnDefinition = "TEXT")
    private String featureVectorJson;

    @Column(name = "explanation", length = 500)
    private String explanation;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    protected void onCreate() {
        this.detectedAt = LocalDateTime.now();
    }
}
