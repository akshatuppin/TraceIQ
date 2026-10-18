package com.traceiq.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders API", description = "Monitored E-Commerce Orders Service")
public class OrderController {

    private final List<Map<String, Object>> mockOrders = new ArrayList<>();

    public OrderController() {
        mockOrders.add(Map.of("id", "ORD-1001", "item", "Cloud Architecture Guide", "amount", 49.99, "status", "CONFIRMED"));
        mockOrders.add(Map.of("id", "ORD-1002", "item", "Java Performance Tuning", "amount", 79.50, "status", "SHIPPED"));
    }

    @GetMapping
    @Operation(summary = "List all orders")
    public ResponseEntity<List<Map<String, Object>>> getOrders() {
        return ResponseEntity.ok(mockOrders);
    }

    @PostMapping
    @Operation(summary = "Place a new customer order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> orderPayload) {
        String orderId = "ORD-" + (1000 + mockOrders.size() + 1);
        Map<String, Object> created = Map.of(
                "id", orderId,
                "item", orderPayload.getOrDefault("item", "Standard Item"),
                "amount", orderPayload.getOrDefault("amount", 29.99),
                "status", "CONFIRMED"
        );
        mockOrders.add(created);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by order ID")
    public ResponseEntity<Map<String, Object>> getOrderById(@PathVariable String id) {
        return mockOrders.stream()
                .filter(o -> o.get("id").equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Order not found with id: " + id)));
    }
}
