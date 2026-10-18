package com.traceiq.core.model.dto;

import com.traceiq.core.model.entity.IncidentEvidence;
import com.traceiq.core.model.entity.IncidentSeverity;
import com.traceiq.core.model.entity.IncidentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IncidentDto {
    private Long id;
    private String incidentKey;
    private Long applicationId;
    private String endpoint;
    private String title;
    private String description;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private String triggeredBy;
    private String assignedTo;
    private String resolutionNotes;
    private LocalDateTime openedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private List<IncidentEvidence> evidence;
}
