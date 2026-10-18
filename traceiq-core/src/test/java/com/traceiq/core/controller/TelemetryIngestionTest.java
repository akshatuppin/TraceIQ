package com.traceiq.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceiq.core.model.dto.TelemetryBatchRequest;
import com.traceiq.core.model.dto.TelemetryEventDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TelemetryIngestionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should accept single telemetry event asynchronously with HTTP 202 Accepted")
    void shouldAcceptSingleEvent() throws Exception {
        TelemetryEventDto dto = TelemetryEventDto.builder()
                .traceId("trc_test_001")
                .applicationId(1L)
                .endpoint("/api/orders")
                .httpMethod("GET")
                .statusCode(200)
                .durationMs(145L)
                .clientIp("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/v1/telemetry/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("Should accept batch telemetry events asynchronously with HTTP 202 Accepted")
    void shouldAcceptBatchEvents() throws Exception {
        TelemetryEventDto event1 = TelemetryEventDto.builder()
                .traceId("trc_batch_001")
                .applicationId(1L)
                .endpoint("/api/orders")
                .httpMethod("POST")
                .statusCode(201)
                .durationMs(220L)
                .build();

        TelemetryEventDto event2 = TelemetryEventDto.builder()
                .traceId("trc_batch_002")
                .applicationId(1L)
                .endpoint("/api/orders")
                .httpMethod("POST")
                .statusCode(500)
                .durationMs(1500L)
                .errorMessage("NullPointerException in OrderValidator")
                .exceptionType("java.lang.NullPointerException")
                .build();

        TelemetryBatchRequest batchRequest = TelemetryBatchRequest.builder()
                .events(List.of(event1, event2))
                .build();

        mockMvc.perform(post("/api/v1/telemetry/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.count").value(2));
    }
}
