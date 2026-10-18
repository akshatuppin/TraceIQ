package com.traceiq.core.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TelemetryBatchRequest {

    @NotEmpty(message = "Events batch cannot be empty")
    @Valid
    private List<TelemetryEventDto> events;
}
