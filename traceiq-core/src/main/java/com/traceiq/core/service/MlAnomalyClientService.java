package com.traceiq.core.service;

import com.traceiq.core.model.entity.AnomalyResult;
import com.traceiq.core.model.entity.MetricWindow;
import com.traceiq.core.repository.AnomalyResultRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class MlAnomalyClientService {

    private final AnomalyResultRepository anomalyResultRepository;

    @Value("${traceiq.ml-service.url:http://localhost:8000}")
    private String mlServiceUrl;

    @Value("${traceiq.ml-service.timeout-ms:1500}")
    private int timeoutMs;

    @Value("${traceiq.ml-service.enabled:true}")
    private boolean mlEnabled;

    @Data
    public static class MlScoreRequest {
        private String endpoint;
        private double rpm;
        private double error_rate;
        private double p95_latency;
        private long status_5xx;
        private long timeouts;
    }

    @Data
    public static class MlScoreResponse {
        private double anomaly_score;
        private boolean is_anomaly;
        private String explanation;
    }

    public AnomalyResult evaluateWindow(MetricWindow window) {
        if (!mlEnabled) {
            return null;
        }

        try {
            RestTemplate restTemplate = new RestTemplateBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .readTimeout(Duration.ofMillis(timeoutMs))
                    .build();

            MlScoreRequest request = new MlScoreRequest();
            request.setEndpoint(window.getEndpoint());
            request.setRpm(window.getRequestCount());
            request.setError_rate(window.getErrorRatePercent());
            request.setP95_latency(window.getP95Ms());
            request.setStatus_5xx(window.getStatus5xxCount());
            request.setTimeouts(window.getTimeoutCount());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MlScoreRequest> entity = new HttpEntity<>(request, headers);

            MlScoreResponse response = restTemplate.postForObject(
                    mlServiceUrl + "/api/v1/anomaly/score",
                    entity,
                    MlScoreResponse.class
            );

            if (response != null) {
                AnomalyResult result = AnomalyResult.builder()
                        .applicationId(window.getApplicationId())
                        .endpoint(window.getEndpoint())
                        .windowStart(window.getWindowStart())
                        .windowEnd(window.getWindowEnd())
                        .anomalyScore(response.getAnomaly_score())
                        .isAnomaly(response.is_anomaly())
                        .explanation(response.getExplanation())
                        .featureVectorJson(String.format("{\"rpm\":%f,\"error_rate\":%f,\"p95\":%f,\"5xx\":%d,\"timeout\":%d}",
                                window.getRequestCount() * 1.0, window.getErrorRatePercent(), window.getP95Ms(),
                                window.getStatus5xxCount(), window.getTimeoutCount()))
                        .build();

                return anomalyResultRepository.save(result);
            }
        } catch (Exception e) {
            log.warn("ML Anomaly detection service unreachable at {} ({}). Gracefully falling back to rule-based engine.",
                    mlServiceUrl, e.getMessage());
        }

        return null;
    }
}
