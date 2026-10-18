package com.traceiq.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory API", description = "Monitored Inventory Stock Level Service")
public class InventoryController {

    @GetMapping("/{sku}")
    @Operation(summary = "Check available stock for an SKU")
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable String sku) {
        return ResponseEntity.ok(Map.of(
                "sku", sku,
                "inStock", true,
                "availableQuantity", 145,
                "warehouse", "CENTRAL-DC-1"
        ));
    }
}
