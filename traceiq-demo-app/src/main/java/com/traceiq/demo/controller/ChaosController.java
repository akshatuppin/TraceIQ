package com.traceiq.demo.controller;

import com.traceiq.demo.chaos.ChaosState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/demo/chaos")
@RequiredArgsConstructor
@Tag(name = "Chaos Failure Simulator", description = "Endpoints to inject latency spikes, error bursts, and simulate outages for live demonstrations")
public class ChaosController {

    private final ChaosState chaosState;

    @PostMapping("/latency")
    @Operation(summary = "Inject artificial latency delay on monitored endpoints")
    public ResponseEntity<Map<String, Object>> injectLatency(@RequestParam(defaultValue = "3000") long delayMs) {
        chaosState.setLatencyDelayMs(delayMs);
        return ResponseEntity.ok(Map.of(
                "status", "CHAOS_LATENCY_ENABLED",
                "injectedDelayMs", delayMs,
                "message", "Monitored endpoints will now experience simulated latency of " + delayMs + " ms"
        ));
    }

    @PostMapping("/errors")
    @Operation(summary = "Inject artificial 500 error rate on monitored endpoints")
    public ResponseEntity<Map<String, Object>> injectErrors(@RequestParam(defaultValue = "50") int rate) {
        chaosState.setErrorRatePercent(rate);
        return ResponseEntity.ok(Map.of(
                "status", "CHAOS_ERRORS_ENABLED",
                "injectedErrorRatePercent", rate,
                "message", "Monitored endpoints will now fail with HTTP 500 errors " + rate + "% of the time"
        ));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset all chaos injections and restore normal operation")
    public ResponseEntity<Map<String, Object>> resetChaos() {
        chaosState.reset();
        return ResponseEntity.ok(Map.of(
                "status", "CHAOS_RESET_COMPLETE",
                "message", "All chaos parameters restored to normal (0ms latency, 0% errors)"
        ));
    }

    @GetMapping("/status")
    @Operation(summary = "Get current status of chaos injection engine")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "latencyDelayMs", chaosState.getLatencyDelayMs(),
                "errorRatePercent", chaosState.getErrorRatePercent(),
                "isChaosActive", chaosState.getLatencyDelayMs() > 0 || chaosState.getErrorRatePercent() > 0
        ));
    }
}
