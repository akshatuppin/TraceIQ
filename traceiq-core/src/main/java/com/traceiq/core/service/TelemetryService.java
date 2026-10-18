package com.traceiq.core.service;

import com.traceiq.core.model.dto.TelemetryBatchRequest;
import com.traceiq.core.model.dto.TelemetryEventDto;
import com.traceiq.core.model.entity.TelemetryEvent;
import com.traceiq.core.repository.TelemetryEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryService {

    private final TelemetryEventRepository telemetryEventRepository;

    private static final Pattern SENSITIVE_DATA_PATTERN =
            Pattern.compile("(?i)(password|token|bearer|secret|authorization|credit_card|cvv)[\"']?\\s*[:=]\\s*[\"']?([^\"'\\s,]+)");

    @Async
    @Transactional
    public void processBatchAsync(TelemetryBatchRequest request) {
        if (request == null || request.getEvents() == null || request.getEvents().isEmpty()) {
            return;
        }

        List<TelemetryEvent> entities = new ArrayList<>(request.getEvents().size());
        for (TelemetryEventDto dto : request.getEvents()) {
            entities.add(mapToEntity(dto));
        }

        telemetryEventRepository.saveAll(entities);
        log.debug("Ingested batch of {} telemetry events asynchronously", entities.size());
    }

    @Async
    @Transactional
    public void processSingleEventAsync(TelemetryEventDto dto) {
        if (dto == null) {
            return;
        }
        TelemetryEvent entity = mapToEntity(dto);
        telemetryEventRepository.save(entity);
        log.debug("Ingested single telemetry event asynchronously: {}", entity.getEndpoint());
    }

    public Page<TelemetryEvent> getEvents(Long applicationId, Pageable pageable) {
        return telemetryEventRepository.findByApplicationId(applicationId, pageable);
    }

    public List<TelemetryEvent> getEventsByTraceId(String traceId) {
        return telemetryEventRepository.findByTraceId(traceId);
    }

    private TelemetryEvent mapToEntity(TelemetryEventDto dto) {
        String traceId = (dto.getTraceId() != null && !dto.getTraceId().isBlank())
                ? dto.getTraceId()
                : UUID.randomUUID().toString().replace("-", "");

        return TelemetryEvent.builder()
                .traceId(traceId)
                .applicationId(dto.getApplicationId())
                .endpoint(dto.getEndpoint())
                .httpMethod(dto.getHttpMethod().toUpperCase())
                .statusCode(dto.getStatusCode())
                .durationMs(dto.getDurationMs())
                .errorMessage(sanitize(dto.getErrorMessage()))
                .exceptionType(dto.getExceptionType())
                .stackTraceSnippet(sanitize(dto.getStackTraceSnippet()))
                .clientIp(dto.getClientIp())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now())
                .build();
    }

    private String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return SENSITIVE_DATA_PATTERN.matcher(text).replaceAll("$1=[REDACTED]");
    }
}
