package com.traceiq.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TelemetryEventDto {

    private String traceId;

    @NotNull(message = "applicationId is required")
    private Long applicationId;

    @NotBlank(message = "endpoint is required")
    private String endpoint;

    @NotBlank(message = "httpMethod is required")
    private String httpMethod;

    @NotNull(message = "statusCode is required")
    private Integer statusCode;

    @NotNull(message = "durationMs is required")
    private Long durationMs;

    private String errorMessage;
    private String exceptionType;
    private String stackTraceSnippet;
    private String clientIp;
    private LocalDateTime timestamp;
}
