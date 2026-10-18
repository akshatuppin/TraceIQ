# TraceIQ: Master Prompts, Context & Engineering Decision Log

> **Purpose:** This file maintains the canonical prompt history, AI system guidelines, architectural decisions, and phase-by-phase instructions for building **TraceIQ (Intelligent API Telemetry & Failure Diagnosis Platform)**. Keep this file updated as implementation milestones are completed.

---

## 1. Context & Genesis (From ChatGPT Planning Session)

- **Source Chat Session:** `https://chatgpt.com/share/6ad4c86a-75ec-83e8-aeb0-3646fbded61b`
- **GitHub Repository:** `https://github.com/akshatuppin/TraceIQ.git` (Remote configured, commits tracked locally; pushing scheduled once the core backend foundation is verified)
- **Candidate Profile:** Final-year IT student targeting Java / Spring Boot enterprise backend placement roles.
- **Prior Experience:** Core Java, Spring Boot, REST APIs, MySQL, Spring Security (JWT), previous banking-platform internship, AI mock interview project.
- **Project Selection Evaluation:**
  - Evaluated **FinGuard** (Financial fraud risk detection) vs. **TraceIQ** (API failure diagnosis & observability).
  - Selected **TraceIQ** because it allows direct, deep demonstration of core Java backend engineering (interceptors, thread pooling, scheduled aggregation, JPA optimization, REST API design, incident lifecycle) alongside a realistic, testable ML component (Isolation Forest for telemetry anomaly scoring).
- **Core Distinction from Existing Tools:**
  - **Swagger UI:** Documents and manually tests individual API requests.
  - **Spring Boot Actuator:** Exposes raw point-in-time system metrics and health flags.
  - **TraceIQ:** Actively monitors traffic, computes rolling performance windows, correlates errors across requests, detects anomalies using ML + rules, generates deduplicated incidents, and provides diagnostic evidence.

---

## 2. Architectural Decision Records (ADRs)

### ADR-01: Modular Monolith vs. Distributed Microservices
- **Decision:** Build TraceIQ as a modular Spring Boot service plus one monitored demo service, rather than a distributed cluster of 8+ microservices.
- **Rationale:** Prevents configuration fatigue, port sprawl, and operational overhead. Allows full focus on writing clean, production-grade Spring Boot code suitable for code reviews in placement interviews.

### ADR-02: Spring Async Buffering over Apache Kafka for MVP
- **Decision:** Ingest events via non-blocking Spring `@Async` thread pools and batch database inserts rather than introducing Apache Kafka in Phase 1.
- **Rationale:** Kafka adds heavy operational complexity (ZooKeeper/KRaft, 1GB+ RAM consumption, cluster configuration) with minimal academic interview advantage for a single demo app. Spring asynchronous executor with graceful shutdown demonstrates strong concurrent Java skills.

### ADR-03: scikit-learn Isolation Forest for ML Anomaly Detection
- **Decision:** Use an unsupervised Isolation Forest model in Python (FastAPI) instead of a generative LLM for core failure detection.
- **Rationale:** Numerical metrics (RPM, error rate, p95 latency, 5xx count) are best evaluated by time-series/tabular anomaly algorithms. Isolation Forest is fast, unsupervised (doesn't require millions of labeled failures), explainable, and testable against static threshold baselines.

### ADR-04: Resilient Fallback Architecture
- **Decision:** If the Python ML anomaly service is unreachable or errors out, TraceIQ must continue operating via its rule-based threshold engine without failing telemetry ingestion or incident creation.
- **Rationale:** Real-world enterprise systems require graceful degradation.

### ADR-05: Git Branching & Push Strategy
- **Decision:** Build and verify solid backend code locally first with disciplined commit hygiene; push side-by-side to `https://github.com/akshatuppin/TraceIQ.git` once core milestones are verified.
- **Rationale:** Ensures the remote repository only receives clean, compiling, and well-tested code without intermediate broken build states.

---

## 3. Master System Prompt for AI Coding Assistants

When prompting an AI assistant (or this agent) in future sessions, provide this system context:

```text
You are a Staff Software Engineer assisting in the development of "TraceIQ", an Intelligent API Telemetry & Failure Diagnosis Platform.

Core Technology Stack:
- Backend: Java 21, Spring Boot 3.4+, Spring Data JPA, Spring Security with JWT
- Database: MySQL 8 (relational schema with proper indexes on timestamps and endpoints)
- ML Service: Python 3.10+, FastAPI, scikit-learn (Isolation Forest)
- Frontend: Modern Web Dashboard (Clean dark theme, real-time metrics, incident triage)
- Deployment: Docker Compose

Core Rules:
1. Adhere strictly to the agreed scope. Do not introduce Kafka, Kubernetes, or complex Java bytecode agents in the MVP.
2. Maintain strong enterprise Java design principles: clean separation of Controller, Service, Repository, DTOs, and Exception Handling.
3. Keep telemetry ingestion asynchronous and non-blocking to ensure zero impact on monitored client applications.
4. Ensure all database interactions utilize indexed columns (e.g., application_id, endpoint, timestamp).
5. Always keep README.md and PROMPT.md updated as modules evolve.
```

---

## 4. Phase-by-Phase Execution Prompts

These structured prompts can be executed sequentially to implement TraceIQ:

### 🔹 Phase 1: Project Scaffolding & Database Tier
```text
Task: Scaffold the TraceIQ project structure and initialize the MySQL database schema.
1. Create a Maven multi-module structure:
   - traceiq-core (Collector, Aggregator, Incident Engine, REST APIs)
   - traceiq-demo-app (Monitored sample e-commerce application)
   - traceiq-ml-service (Python FastAPI anomaly detection service)
2. Define MySQL schema and Flyway migration scripts for:
   - users, monitored_applications, telemetry_events, metric_windows, incidents, incident_evidence, anomaly_results, audit_logs.
3. Configure Spring Security with JWT authentication and BCrypt password encryption.
```

### 🔹 Phase 2: Telemetry Ingestion & Monitored Demo Application
```text
Task: Implement the telemetry ingestion pipeline and sample e-commerce application.
1. In traceiq-core:
   - Create POST /api/v1/telemetry/events endpoint accepting batches of request logs.
   - Implement asynchronous worker thread pool using Spring @Async and ThreadPoolTaskExecutor for high-throughput database writes.
2. In traceiq-demo-app:
   - Create sample endpoints: GET /api/orders, POST /api/orders, GET /api/inventory, POST /api/payments.
   - Implement a Spring HandlerInterceptor / Filter that measures latency, captures status code, extracts correlation ID, and dispatches telemetry asynchronously to traceiq-core.
   - Implement /api/demo/chaos endpoints to simulate latency spikes, 500 errors, and random database timeouts.
```

### 🔹 Phase 3: Rolling Window Metrics & Rule-Based Incident Engine
```text
Task: Build the metric aggregation scheduler and rule-based incident lifecycle engine.
1. Implement a Spring @Scheduled task running every 60 seconds to aggregate raw telemetry_events into metric_windows (RPM, error rate %, p50, p95, p99, 5xx count).
2. Build the Incident Engine:
   - Configurable rules: Error Rate > 10%, P95 Latency > 2000ms, Consecutive 500s > 5.
   - Implement deduplication keys (application_id + endpoint + failure_type) to prevent alert flooding.
   - Manage status lifecycle: OPEN -> ACKNOWLEDGED -> RESOLVED.
   - Correlate supporting evidence (matching stack traces, slow traces) into incident_evidence.
```

### 🔹 Phase 4: Python ML Anomaly Detection Service
```text
Task: Build and integrate the Python FastAPI Isolation Forest anomaly service.
1. In traceiq-ml-service:
   - Create FastAPI endpoint POST /api/v1/anomaly/score receiving rolling metric vectors: [rpm, error_rate, p95_latency, 5xx_count, timeout_count].
   - Implement scikit-learn Isolation Forest model with anomaly scoring and contribution breakdown.
   - Provide training/synthetic baseline data generator for normal vs anomalous traffic.
2. In traceiq-core:
   - Add RestClient/WebClient integration to invoke the ML service on each metric window.
   - Store predictions in anomaly_results.
   - Add graceful circuit breaker fallback if ML service is unreachable.
```

### 🔹 Phase 5: Web Dashboard & Diagnostic Interface
```text
Task: Build the modern web frontend for TraceIQ.
1. Setup a modern dashboard:
   - Live endpoint health cards (status, uptime, average latency, error rate).
   - Interactive metric charts (time-series of RPM and P95 latency).
   - Incident triage board (severity tags, status toggles, evidence drawer).
   - Detailed incident modal displaying root cause hypothesis, correlated stack traces, and affected time windows.
```

### 🔹 Phase 6: Containerization, Testing & Interview Demonstration Script
```text
Task: Containerize the platform and finalize demonstration scripts.
1. Write Dockerfile for traceiq-core, traceiq-demo-app, traceiq-ml-service, and frontend.
2. Create root docker-compose.yml including MySQL 8 with healthchecks.
3. Write end-to-end integration tests using Testcontainers or MockMvc.
4. Prepare the 3-step live demo script:
   - Scenario A: Normal traffic.
   - Scenario B: Injected latency spike on /api/orders.
   - Scenario C: Injected 500 error storm and anomaly detection trigger.
```

---

## 5. Placement & Viva Interview Defense Cheatsheet

| Common Interviewer Question | Recommended Architectural Answer |
|---|---|
| *"Why build this when Spring Boot Actuator already exists?"* | *"Actuator exposes raw system metrics and health endpoints at a single point in time, but it doesn't correlate errors across requests, compute time-windowed incident trends, detect multi-dimensional anomalies, or manage an incident resolution lifecycle. TraceIQ uses telemetry (which can include Actuator data) to perform automated failure investigation."* |
| *"Why not use an existing tool like Prometheus or Datadog?"* | *"Enterprise tools exist, but building TraceIQ demonstrates deep knowledge of how telemetry pipelines operate internally: request interception, asynchronous batch persistence, rolling percentile calculations, correlation ID propagation, and machine learning integration on time-series telemetry."* |
| *"Why did you use Isolation Forest instead of an LLM for anomaly detection?"* | *"Telemetry data is numerical and time-series based. Isolation Forest is specifically suited for high-dimensional numerical outlier detection without requiring labeled training datasets or expensive GPU inference. An LLM would be over-engineered, slow, and expensive for 1-minute window evaluations, though an LLM could be added optionally as a summarizer of incident evidence."* |
| *"How do you ensure the monitoring doesn't slow down the main application?"* | *"Telemetry capture in the demo app is lightweight: start and end timestamps are recorded in a ThreadLocal/interceptor, and the telemetry payload is dispatched to a background thread pool or asynchronous queue. The main HTTP response is never blocked by TraceIQ."* |

---

## 6. Change Log & Updates

- **2026-10-18:** Initialized project scope, architecture blueprints, ADRs, `README.md`, and `PROMPT.md` based on ChatGPT requirements analysis.
- **2026-10-18:** Linked remote Git repository `https://github.com/akshatuppin/TraceIQ.git`, drafted detailed Backend Requirements Specification (10 FRs, 8 NFRs, REST API table, schema specs), and successfully pushed the initial foundation to `main`.
- **2026-10-18:** **Completed Backend Implementation:**
  - Scaffolded Maven multi-module parent POM, `traceiq-core`, and `traceiq-demo-app`.
  - Implemented 8 JPA entities (`User`, `MonitoredApplication`, `TelemetryEvent`, `MetricWindow`, `Incident`, `IncidentEvidence`, `AnomalyResult`, `AuditLog`) with composite indexes.
  - Implemented stateless Spring Security 6 with JJWT 0.12+ Bearer authentication and RBAC (`ROLE_ADMIN`, `ROLE_DEVELOPER`, `ROLE_VIEWER`).
  - Built high-throughput non-blocking telemetry ingestion pipeline (`@Async` thread pool + data sanitization).
  - Built rolling metric aggregation engine (`@Scheduled` 60s windows, percentiles, error rates) and dual incident detection (rules + ML Isolation Forest client with circuit fallback).
  - Built incident lifecycle manager (`OPEN` -> `ACKNOWLEDGED` -> `RESOLVED`), deduplication engine with 15-min cooldown, and evidence correlation.
  - Built `traceiq-demo-app` with e-commerce endpoints (`/orders`, `/payments`, `/inventory`), `TraceIqTelemetryInterceptor` (< 3ms overhead), and `ChaosController` (latency injection, 500 error bursts).
  - 100% test pass rate across `AuthControllerTest`, `TelemetryIngestionTest`, and `IncidentServiceTest`. Ready to push.
