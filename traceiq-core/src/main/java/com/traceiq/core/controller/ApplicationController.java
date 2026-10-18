package com.traceiq.core.controller;

import com.traceiq.core.model.dto.ApplicationRequest;
import com.traceiq.core.model.dto.ApplicationResponse;
import com.traceiq.core.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Monitored Applications", description = "Register and manage applications tracked by TraceIQ")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @Operation(summary = "Register a new monitored application and generate API credentials")
    public ResponseEntity<ApplicationResponse> registerApplication(@Valid @RequestBody ApplicationRequest request) {
        ApplicationResponse response = applicationService.registerApplication(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all monitored applications")
    public ResponseEntity<List<ApplicationResponse>> getAllApplications() {
        List<ApplicationResponse> apps = applicationService.getAllApplications();
        return ResponseEntity.ok(apps);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get details of a monitored application by ID")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable Long id) {
        ApplicationResponse app = applicationService.getApplicationById(id);
        return ResponseEntity.ok(app);
    }
}
