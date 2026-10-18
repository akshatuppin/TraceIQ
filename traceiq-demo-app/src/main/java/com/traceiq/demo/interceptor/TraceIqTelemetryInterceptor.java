package com.traceiq.demo.interceptor;

import com.traceiq.demo.chaos.ChaosState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TraceIqTelemetryInterceptor implements HandlerInterceptor {

    private final ChaosState chaosState;

    @Value("${traceiq.client.collector-url:http://localhost:8080/api/v1/telemetry/event}")
    private String collectorUrl;

    @Value("${traceiq.client.application-id:1}")
    private Long applicationId;

    @Value("${traceiq.client.app-key:trc_live_demo_key_98234}")
    private String appKey;

    @Value("${traceiq.client.enabled:true}")
    private boolean telemetryEnabled;

    private final ExecutorService asyncDispatcher = Executors.newFixedThreadPool(4);
    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .connectTimeout(Duration.ofMillis(1000))
            .readTimeout(Duration.ofMillis(1000))
            .build();

    private static final String START_TIME_ATTR = "TRACEIQ_START_TIME";
    private static final String TRACE_ID_ATTR = "TRACEIQ_TRACE_ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTR, startTime);

        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        request.setAttribute(TRACE_ID_ATTR, traceId);
        response.setHeader("X-Trace-Id", traceId);

        // Apply Chaos Latency Injection if configured
        long delayMs = chaosState.getLatencyDelayMs();
        if (delayMs > 0 && !request.getRequestURI().startsWith("/api/demo/chaos")) {
            log.warn("Injecting chaos latency delay of {} ms on {}", delayMs, request.getRequestURI());
            Thread.sleep(delayMs);
        }

        // Apply Chaos Error Injection if configured
        int errorRate = chaosState.getErrorRatePercent();
        if (errorRate > 0 && !request.getRequestURI().startsWith("/api/demo/chaos")) {
            int randomRoll = (int) (Math.random() * 100);
            if (randomRoll < errorRate) {
                log.warn("Injecting simulated failure on {} (Roll: {} vs Rate: {})", request.getRequestURI(), randomRoll, errorRate);
                throw new RuntimeException("Simulated Chaos Failure: Downstream service unavailable or database timeout exception");
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!telemetryEnabled) {
            return;
        }

        // Ignore internal swagger and chaos endpoints to avoid telemetry noise
        String uri = request.getRequestURI();
        if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs") || uri.startsWith("/api/demo/chaos")) {
            return;
        }

        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long duration = startTime != null ? (System.currentTimeMillis() - startTime) : 0L;
        String traceId = (String) request.getAttribute(TRACE_ID_ATTR);
        int statusCode = response.getStatus();

        String exceptionType = null;
        String errorMessage = null;
        String stackTrace = null;

        if (ex != null) {
            exceptionType = ex.getClass().getName();
            errorMessage = ex.getMessage();
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            stackTrace = sw.toString();
            if (statusCode < 400) {
                statusCode = 500;
            }
        }

        final int finalStatus = statusCode;
        final String finalException = exceptionType;
        final String finalErrorMessage = errorMessage;
        final String finalStackTrace = stackTrace;

        // Dispatch telemetry asynchronously to background thread pool
        asyncDispatcher.submit(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("traceId", traceId);
                payload.put("applicationId", applicationId);
                payload.put("endpoint", uri);
                payload.put("httpMethod", request.getMethod());
                payload.put("statusCode", finalStatus);
                payload.put("durationMs", duration);
                payload.put("clientIp", request.getRemoteAddr());
                payload.put("timestamp", LocalDateTime.now().toString());

                if (finalException != null) {
                    payload.put("exceptionType", finalException);
                    payload.put("errorMessage", finalErrorMessage);
                    payload.put("stackTraceSnippet", finalStackTrace);
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-TraceIQ-App-Key", appKey);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                restTemplate.postForEntity(collectorUrl, entity, String.class);
            } catch (Exception dispatchError) {
                log.debug("Telemetry dispatch skipped or failed (TraceIQ collector might be offline): {}", dispatchError.getMessage());
            }
        });
    }
}
