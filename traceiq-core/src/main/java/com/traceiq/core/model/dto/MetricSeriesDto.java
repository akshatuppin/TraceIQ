package com.traceiq.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetricSeriesDto {
    private LocalDateTime timestamp;
    private String endpoint;
    private long requestCount;
    private double errorRatePercent;
    private double avgLatencyMs;
    private double p95LatencyMs;
    private long status5xxCount;
}
