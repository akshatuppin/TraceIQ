package com.traceiq.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments API", description = "Monitored Payment Gateway Simulator")
public class PaymentController {

    @PostMapping
    @Operation(summary = "Process a simulated payment")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> paymentPayload) {
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return ResponseEntity.ok(Map.of(
                "transactionId", transactionId,
                "amount", paymentPayload.getOrDefault("amount", 100.0),
                "status", "SUCCESS",
                "message", "Payment authorized successfully"
        ));
    }
}
