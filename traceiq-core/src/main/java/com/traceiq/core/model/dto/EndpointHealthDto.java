package com.traceiq.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EndpointHealthDto {
    private String endpoint;
    private long totalRequests;
    private double errorRatePercent;
    private double avgLatencyMs;
    private double p95LatencyMs;
    private long status5xxCount;
    private String healthStatus; // HEALTHY, DEGRADED, CRITICAL
    private long activeIncidentsCount;
}
