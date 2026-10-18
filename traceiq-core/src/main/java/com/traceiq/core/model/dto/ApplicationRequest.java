package com.traceiq.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationRequest {

    @NotBlank(message = "appName is required")
    private String appName;

    @Builder.Default
    private String environment = "production";

    private String description;
}
