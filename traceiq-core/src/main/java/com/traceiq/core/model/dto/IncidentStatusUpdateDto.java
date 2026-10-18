package com.traceiq.core.model.dto;

import com.traceiq.core.model.entity.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IncidentStatusUpdateDto {
    @NotNull(message = "status is required")
    private IncidentStatus status;

    private String notes;
    private String assignedTo;
}
