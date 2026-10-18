package com.traceiq.core.service;

import com.traceiq.core.exception.BadRequestException;
import com.traceiq.core.exception.ResourceNotFoundException;
import com.traceiq.core.model.dto.ApplicationRequest;
import com.traceiq.core.model.dto.ApplicationResponse;
import com.traceiq.core.model.entity.MonitoredApplication;
import com.traceiq.core.repository.MonitoredApplicationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final MonitoredApplicationRepository applicationRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void seedDefaultApplication() {
        if (applicationRepository.count() == 0) {
            MonitoredApplication defaultApp = MonitoredApplication.builder()
                    .appName("traceiq-demo-store")
                    .appKey("trc_live_demo_key_98234")
                    .appSecret("trc_secret_abc1234567890def")
                    .environment("production")
                    .description("Default Monitored E-Commerce Demo Application")
                    .active(true)
                    .build();
            applicationRepository.save(defaultApp);
            log.info("Initialized default monitored application: 'traceiq-demo-store' (appKey: 'trc_live_demo_key_98234')");
        }
    }

    @Transactional
    public ApplicationResponse registerApplication(ApplicationRequest request) {
        if (applicationRepository.existsByAppName(request.getAppName())) {
            throw new BadRequestException("Application with name '" + request.getAppName() + "' is already registered");
        }

        byte[] keyBytes = new byte[16];
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(keyBytes);
        secureRandom.nextBytes(secretBytes);

        String appKey = "trc_live_" + HexFormat.of().formatHex(keyBytes);
        String appSecret = "trc_sec_" + HexFormat.of().formatHex(secretBytes);

        MonitoredApplication application = MonitoredApplication.builder()
                .appName(request.getAppName())
                .appKey(appKey)
                .appSecret(appSecret)
                .environment(request.getEnvironment() != null ? request.getEnvironment() : "production")
                .description(request.getDescription())
                .active(true)
                .build();

        MonitoredApplication saved = applicationRepository.save(application);
        return mapToResponse(saved);
    }

    public List<ApplicationResponse> getAllApplications() {
        return applicationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ApplicationResponse getApplicationById(Long id) {
        MonitoredApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitored application not found with id: " + id));
        return mapToResponse(app);
    }

    public MonitoredApplication getApplicationEntity(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitored application not found with id: " + id));
    }

    private ApplicationResponse mapToResponse(MonitoredApplication app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .appName(app.getAppName())
                .appKey(app.getAppKey())
                .appSecret(app.getAppSecret())
                .environment(app.getEnvironment())
                .description(app.getDescription())
                .active(app.isActive())
                .createdAt(app.getCreatedAt())
                .build();
    }
}
