# TraceIQ: Intelligent API Telemetry & Failure Diagnosis Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Security-JWT%20RBAC-blue.svg)](https://spring.io/projects/spring-security)
[![MySQL](https://img.shields.io/badge/Database-MySQL%208-blue.svg)](https://www.mysql.com/)
[![Python](https://img.shields.io/badge/Python-FastAPI%20%7C%20scikit--learn-yellow.svg)](https://fastapi.tiangolo.com/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

---

## 📌 Executive Summary

**TraceIQ** is a targeted observability and failure diagnosis platform designed specifically for Spring Boot enterprise applications. It bridges the gap between passive endpoint documentation (Swagger/OpenAPI) and low-level metric exposition (Spring Boot Actuator) by providing an **automated failure investigation layer**. 

TraceIQ intercepts HTTP requests, correlates errors with traces and timestamps, computes rolling metric windows, runs **ML-based anomaly detection (Isolation Forest)**, triggers deduplicated incidents, and delivers evidence-backed root-cause suggestions directly to developers.

---

## 🧭 The Core Problem & Value Proposition

In modern web backends, troubleshooting service degradation or intermittent 500 errors often requires developers to manually SSH into servers, tail log files, correlate timestamps across disparate systems, and guess the root cause. 

```
┌────────────────────────┐      ┌────────────────────────┐      ┌─────────────────────────┐
│       Swagger UI       │      │  Spring Boot Actuator  │      │         TraceIQ         │
│  (API Documentation)   │      │   (Raw System Metrics) │      │  (Diagnosis & Insights) │
├────────────────────────┤      ├────────────────────────┤      ├─────────────────────────┤
│ • What endpoints exist?│      │ • Exposes raw CPU/RAM  │      │ • Correlates error bursts│
│ • Try requests manually│      │ • Metric endpoints     │      │ • Rolling window metrics│
│ • Shows single response│      │ • Health check flags   │      │ • ML anomaly detection  │
│ ✗ No failure history   │      │ ✗ No incident lifecycle│      │ • Automated incidents   │
│ ✗ No anomaly detection │      │ ✗ No root-cause linking│      │ • Evidence-backed triage │
└────────────────────────┘      └────────────────────────┘      └─────────────────────────┘
```

TraceIQ complements Swagger (used to document TraceIQ's own REST APIs) and Actuator (providing foundational telemetry) by serving as the **analytical and diagnostic brain**.

---

## ⚖️ Strategic Scope: What to Maintain vs. What NOT to Maintain

To guarantee a **high-impact, defensible, and realistic** final-year engineering project for Java/Spring Boot placement interviews, the following strict boundaries are established:

### ✅ What to Maintain (Core Scope - In Priority Order)

1. **Modular Java / Spring Boot Backend (TraceIQ Server):**
   - **Telemetry Ingestion Engine:** High-throughput, non-blocking REST endpoints to ingest request logs, execution durations, HTTP status codes, exceptions, and correlation IDs.
   - **Metrics Aggregator:** Asynchronous scheduled job computing 1-minute rolling windows per endpoint (RPM, avg latency, p95 latency, error %, 5xx count).
   - **Incident Management Lifecycle:** Automatic incident creation when thresholds (rules or ML scores) breach, featuring deduplication keys, cooldown windows, severity classification (LOW, MEDIUM, HIGH, CRITICAL), and status transitions (`OPEN` → `ACKNOWLEDGED` → `RESOLVED`).
   - **Evidence Correlation Engine:** Correlates exceptions, stack traces, slow DB queries, and dependent call failures belonging to the same incident window.
   - **Spring Security & JWT RBAC:** Secure multi-tenant or role-based access control (`ROLE_ADMIN`, `ROLE_DEVELOPER`, `ROLE_VIEWER`).
   - **Clean OpenAPI / Swagger Integration:** Full contract documentation for TraceIQ's internal APIs.

2. **Monitored Demo Spring Boot Application:**
   - A standalone, realistic e-commerce application (`Order Service`, `Payment Service`, `Inventory Service`) instrumented via Spring Web Interceptors / Servlet Filters.
   - Built-in failure simulation endpoints (`/api/demo/slowdown`, `/api/demo/errors`, `/api/demo/traffic-spike`) for live placement demonstrations.

3. **Isolated Python ML Anomaly Detection Service:**
   - Lightweight FastAPI service implementing **Isolation Forest** from `scikit-learn`.
   - Analyzes multi-dimensional rolling vectors: `[rpm, error_rate, p95_latency, 5xx_count, timeout_count]`.
   - Returns anomaly scores, threshold triggers, and feature contributions.
   - **Resilience:** If the Python ML service is offline, TraceIQ seamlessly falls back to rule-based detection.

4. **Interactive Dashboard:**
   - Live endpoint health table, metric time-series charts (latency percentiles, request volume, error rates), active incident alerts, and diagnostic drill-downs.

5. **Containerized Deployment:**
   - Clean `docker-compose.yml` spinning up MySQL 8, TraceIQ Core, Monitored Demo App, and Python ML Service.

---

### ❌ What NOT to Maintain (Out of Scope / Strategic Exclusions)

| Avoided Technology / Feature | Rationale for Exclusion |
|---|---|
| **FinGuard / Fraud Detection Features** | FinGuard solves financial risk; TraceIQ solves software observability. Merging them ruins architecture clarity and dilutes interview focus. |
| **Custom Bytecode Java Agent (ASM/ByteBuddy)** | Writing a low-level Java agent from scratch is brittle, prone to classloader leaks, and distracts from enterprise Spring Boot mastery. Use standard Spring filters/interceptors instead. |
| **Apache Kafka / Distributed Message Brokers (in MVP)** | Introduces unnecessary infrastructure overhead (ZooKeeper/KRaft, memory footprint, port conflicts) for a solo developer. Use Spring's native asynchronous processing (`@Async`, thread pools) and database queues first. Kafka is deferred to an optional post-MVP milestone. |
| **Kubernetes & Microservice Sprawl** | Avoid splitting the demo into 10 separate microservices. A dual-application setup (Demo App + TraceIQ Collector) provides the exact same telemetry demonstration with 90% less operational friction. |
| **Autonomous AI Code Patching / Auto-Fixing** | LLMs cannot reliably patch production Java code autonomously without severe risk. TraceIQ diagnoses and presents evidence; the human developer verifies and applies the fix. |
| **Sensitive Payload Persistence** | Never capture or store sensitive request bodies, passwords, or authorization headers in telemetry tables (OWASP compliance). |

---

## 🏛️ System Architecture

```mermaid
flowchart TD
    subgraph Client Traffic
        Client[End User / Load Generator]
    end

    subgraph Monitored Spring Boot Application
        Filter[TraceIQ Telemetry Interceptor / Filter]
        BusinessLogic[Demo APIs: /orders, /payments, /inventory]
        ChaosController[Failure Simulation Controller]
    end

    subgraph TraceIQ Core System (Java 21 + Spring Boot 3.4+)
        Collector[Telemetry Ingestion Controller]
        AsyncQueue[Async Event Dispatcher / Thread Pool]
        MetricsEngine[Rolling Window Metrics Aggregator]
        RuleEngine[Rule-Based Threshold Evaluator]
        IncidentMgr[Incident Lifecycle & Evidence Correlator]
        SecuredAPI[Diagnostic REST APIs (JWT Protected)]
    end

    subgraph Data Tier
        MySQL[(MySQL 8 Database)]
    end

    subgraph Machine Learning Service
        FastAPI[Python FastAPI Service]
        IsoForest[scikit-learn Isolation Forest]
    end

    subgraph Web Frontend
        Dashboard[TraceIQ Modern Web Dashboard]
    end

    Client -->|HTTP Requests| BusinessLogic
    ChaosController -.->|Inject Latency/Errors| BusinessLogic
    BusinessLogic --> Filter
    Filter -->|Async Telemetry Payload + Correlation ID| Collector

    Collector --> AsyncQueue
    AsyncQueue -->|Batch Insert| MySQL
    
    MetricsEngine -->|Query 1-min Window| MySQL
    MetricsEngine -->|Window Feature Vector| FastAPI
    FastAPI --> IsoForest
    IsoForest -->|Anomaly Score & Flag| MetricsEngine

    MetricsEngine --> RuleEngine
    RuleEngine --> IncidentMgr
    IncidentMgr -->|Persist Incidents & Evidence| MySQL

    Dashboard -->|JWT Auth & REST API Queries| SecuredAPI
    SecuredAPI -->|Fetch Telemetry, Incidents & Metrics| MySQL
```

---

---

## 📋 Detailed Backend Requirements Specification

To ensure a robust, production-grade enterprise backend, the requirements for TraceIQ are strictly defined across Functional (FR), Non-Functional (NFR), and Data Contract layers.

### 1. Functional Requirements (FR)

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                TraceIQ Core Capabilities                               │
├─────────────────────┬──────────────────────┬────────────────────┬──────────────────────┤
│ 1. Telemetry Ingest │ 2. Metrics Engine    │ 3. Failure Engine  │ 4. Triage API        │
│ • Batch JSON ingest │ • 1-min aggregations │ • Rule thresholds  │ • Filterable queries │
│ • Async thread pool │ • Percentiles (p95)  │ • ML IsolationFor. │ • Incident lifecycle │
│ • Correlation IDs   │ • Error rate %       │ • Deduplication    │ • Evidence linkage   │
└─────────────────────┴──────────────────────┴────────────────────┴──────────────────────┘
```

- **FR-01: Monitored Application Registration & Authentication**
  - Admins can register applications to be monitored, generating a unique `app_key` and `app_secret`.
  - Spring Security stateless JWT authentication for dashboard users with Role-Based Access Control (`ROLE_ADMIN`, `ROLE_DEVELOPER`, `ROLE_VIEWER`).
- **FR-02: High-Throughput Telemetry Ingestion Pipeline**
  - Endpoints `POST /api/v1/telemetry/events` (batch) and `POST /api/v1/telemetry/event` (single).
  - Validation of incoming payloads (presence of `traceId`, `endpoint`, `httpMethod`, `durationMs`, `statusCode`, `timestamp`).
  - Asynchronous event buffering using Spring's `ThreadPoolTaskExecutor` to guarantee zero blocking on calling applications.
  - Sanitization filter stripping passwords, tokens, and authorization headers from event payloads.
- **FR-03: Real-Time Rolling Metric Aggregation Engine**
  - Scheduled background worker (`@Scheduled`) executing every 60 seconds.
  - Computes per-endpoint metrics: Request Count (RPM), Average Latency, P50, P95, and P99 percentiles, Error Rate percentage, and 5xx count.
  - Persists aggregated snapshots into `metric_windows` for rapid dashboard querying without scanning raw logs.
- **FR-04: Dual Anomaly & Incident Detection Engine**
  - **Rule-Based Engine:** Evaluates configurable static thresholds (e.g., Error Rate > 10%, P95 Latency > 2,000ms, Consecutive 500s > 5).
  - **ML-Based Engine:** Invokes the Python Isolation Forest model passing 1-minute rolling feature vectors.
  - **Deduplication & Cooldown:** Employs a composite key `(application_id + endpoint + issue_type)` with a configurable cooldown period (e.g., 15 minutes) to prevent alert storms during sustained outages.
- **FR-05: Incident Lifecycle & Workflow Management**
  - Automatic incident provisioning with computed severity (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
  - Strict lifecycle transitions: `OPEN` $\rightarrow$ `ACKNOWLEDGED` $\rightarrow$ `RESOLVED`.
  - Captures developer triage notes, assigned engineer, and resolution timestamps.
  - Audit log entries recorded for every state change.
- **FR-06: Root-Cause Evidence Correlation Engine**
  - Links telemetry evidence to an open incident:
    - Top repeating exceptions and stack traces within the incident window.
    - Slowest requests sharing the same `trace_id`.
    - Downstream dependency or database query delay signals.
- **FR-07: Resilient Python ML Service Integration**
  - Dedicated REST client (`RestClient` / `WebClient`) calling the FastAPI service.
  - Configurable timeouts (e.g., 1,500ms) with circuit-breaker fallback to pure rule-based detection if the ML container is unreachable or slow.
- **FR-08: Diagnostic Query APIs**
  - Paginated REST endpoints to query incidents, metric time series, endpoint health, and audit trails.
  - Support for multi-criteria filters: application ID, endpoint, time range (last 15m, 1h, 24h), and severity.
- **FR-09: Monitored Demo E-Commerce Application (`traceiq-demo-app`)**
  - Independent Spring Boot microservice simulating realistic e-commerce traffic:
    - `POST /api/orders` (Order creation)
    - `GET /api/orders/{id}` (Order retrieval)
    - `POST /api/payments` (Payment processing)
    - `GET /api/inventory/{sku}` (Stock check)
  - Intercepted via a custom Spring `HandlerInterceptor` that dispatches telemetry asynchronously.
- **FR-10: Chaos Simulation Engine**
  - Dedicated endpoints on the demo app to trigger deterministic failure patterns during live demos:
    - `POST /api/demo/chaos/latency?delayMs=3000` (Simulates DB contention)
    - `POST /api/demo/chaos/errors?rate=50` (Simulates downstream 500 crashes)
    - `POST /api/demo/chaos/reset` (Restores healthy state)

---

### 2. Non-Functional Requirements (NFR)

| ID | Category | Requirement Specification |
|---|---|---|
| **NFR-01** | **Ingestion Latency** | Telemetry ingestion endpoint (`POST /events`) must return HTTP 202 Accepted in $< 25\text{ ms}$ under normal load by handing payloads off to an async memory queue. |
| **NFR-02** | **Client Zero-Overhead** | The client interceptor running in the monitored application must introduce $< 3\text{ ms}$ of overhead to original requests. |
| **NFR-03** | **Concurrency & Thread Safety** | The backend thread pool must handle up to 500 concurrent events/sec without thread exhaustion using a bounded queue and rejection handler. |
| **NFR-04** | **Data Resilience & Integrity** | Relational integrity enforced via foreign keys, unique constraint indexes, and transactional boundaries (`@Transactional`) on incident updates. |
| **NFR-05** | **Security Standards** | Passwords hashed using BCrypt (cost 12). JWT tokens signed using HMAC-SHA256 with 24-hour expiration. OWASP sanitization on all error message inputs. |
| **NFR-06** | **Fault Tolerance** | An outage in the Python ML service or the dashboard must never disrupt telemetry ingestion or degrade monitored application traffic. |
| **NFR-07** | **API Documentation** | 100% of REST endpoints documented with OpenAPI 3.0 annotations and interactive Swagger UI accessible at `/swagger-ui.html`. |
| **NFR-08** | **Maintainability & Testing** | Unit test coverage $> 80\%$ on core services using JUnit 5 and Mockito; integration tests using Testcontainers or H2 in-memory mode. |

---

### 3. Backend REST API Specification

| Module | Method | Endpoint Path | Auth Required | Description |
|---|---|---|---|---|
| **Auth** | `POST` | `/api/v1/auth/register` | No | Register new developer account |
| **Auth** | `POST` | `/api/v1/auth/login` | No | Authenticate user & return JWT token |
| **Telemetry** | `POST` | `/api/v1/telemetry/events` | App Key | Ingest batch of telemetry events (Async) |
| **Telemetry** | `GET` | `/api/v1/telemetry/events` | `ROLE_DEV` | Query raw events with filtering & pagination |
| **Metrics** | `GET` | `/api/v1/metrics/endpoints` | `ROLE_DEV` | Get aggregated health cards for all endpoints |
| **Metrics** | `GET` | `/api/v1/metrics/timeseries` | `ROLE_DEV` | Fetch 1-min metric series for dashboard charts |
| **Incidents** | `GET` | `/api/v1/incidents` | `ROLE_DEV` | List incidents filtered by status & severity |
| **Incidents** | `GET` | `/api/v1/incidents/{id}` | `ROLE_DEV` | Get incident details with correlated evidence |
| **Incidents** | `PATCH`| `/api/v1/incidents/{id}/status` | `ROLE_DEV` | Update incident status (`ACKNOWLEDGED` / `RESOLVED`) |
| **Apps** | `POST` | `/api/v1/applications` | `ROLE_ADMIN` | Register new application to monitor |
| **Apps** | `GET` | `/api/v1/applications` | `ROLE_DEV` | List all registered applications |
| **Demo Chaos**| `POST` | `/api/demo/chaos/{type}` | No (Demo Only) | Inject latency, 500s, or traffic burst |

---

## 🗄️ Database Schema & Core Entities

TraceIQ stores relational telemetry and diagnostic records in MySQL using JPA / Hibernate:

1. **`users`**: User identity, hashed credentials (BCrypt), roles (`ROLE_ADMIN`, `ROLE_DEVELOPER`).
2. **`monitored_applications`**: Registered services, application keys, environment tags.
3. **`telemetry_events`**: High-cardinality request events (`trace_id`, `endpoint`, `http_method`, `status_code`, `duration_ms`, `error_type`, `exception_message`, `timestamp`). Indexed on `(application_id, timestamp)` and `(endpoint, timestamp)`.
4. **`metric_windows`**: 1-minute aggregated slices (`application_id`, `endpoint`, `window_start`, `window_end`, `request_count`, `error_rate`, `p50_ms`, `p95_ms`, `p99_ms`).
5. **`incidents`**: Identified problems (`incident_key`, `title`, `severity`, `status`, `triggered_by`, `opened_at`, `resolved_at`).
6. **`incident_evidence`**: Correlated logs, stack traces, and affected metrics tied to an incident.
7. **`anomaly_results`**: Output scores from the Python Isolation Forest model (`score`, `is_anomaly`, `feature_vector`).
8. **`audit_logs`**: Historical record of developer triage actions (e.g., acknowledged, severity updated, resolved).

---

## 🔬 Machine Learning Anomaly Detection Design

Instead of attempting fragile NLP on raw code, TraceIQ focuses on **numerical metric anomaly detection**:

- **Model:** `IsolationForest(n_estimators=100, contamination=0.05, random_state=42)`
- **Input Feature Vector (1-Minute Intervals):**
  $$\mathbf{x} = [\text{RPM}, \text{ErrorRate}\%, \text{P95\_Latency}, \text{HTTP\_5xx\_Count}, \text{Timeout\_Count}]$$
- **Baseline Comparison:** The project compares Isolation Forest against static thresholding (e.g., Error Rate > 5% or P95 > 1500ms) to evaluate:
  - Detection Latency (time to identify anomalous pattern)
  - False Positive Rate under natural traffic spikes
  - Precision, Recall, and F1 Score on synthetic labeled benchmark scenarios.

---

## 🎯 3 Live Placement Demonstration Scenarios

| Scenario | Simulated Condition | Expected TraceIQ System Behavior |
|---|---|---|
| **1. Normal Operation** | Standard traffic across `/orders` and `/inventory` (200ms avg latency, 0% error rate). | Metrics update smoothly; endpoint status flags `HEALTHY`; no incidents triggered. |
| **2. Database Slowdown / Latency Spike** | Chaos trigger simulates slow DB queries on `/orders` (latency climbs to 4,200ms). | Rolling P95 exceeds baseline; Isolation Forest flags latency anomaly; HIGH severity Incident created with DB timeout evidence. |
| **3. Cascading Failure / 500 Burst** | Chaos trigger introduces unexpected `NullPointerException` or downstream payment timeout. | Error rate spikes to 35%; CRITICAL Incident auto-created; correlated stack traces grouped into the incident evidence tab. |

---

## 🗓️ 12-Week Implementation Roadmap

- **Weeks 1–2: Project Foundation & Security**
  - Git repository setup, Maven multi-module structure, MySQL schema migrations (Flyway).
  - Spring Security with JWT authentication and role-based endpoints.
- **Weeks 3–4: Telemetry Ingestion & Demo Application**
  - Build monitored demo e-commerce service with chaos injection endpoints.
  - Implement TraceIQ Client Telemetry Interceptor and TraceIQ Ingestion Controller with asynchronous buffering.
- **Weeks 5–6: Rolling Metrics & Rule-Based Incident Engine**
  - Scheduled window aggregation service (cron / Spring `@Scheduled`).
  - Rule-based threshold evaluator and incident deduplication engine.
- **Weeks 7–8: Python ML Anomaly Service & Evaluation**
  - Develop FastAPI service with scikit-learn Isolation Forest.
  - REST client integration between Spring Boot and FastAPI; fallback circuit breaker.
  - Benchmark performance against static threshold baseline.
- **Weeks 9–10: Web Dashboard & Diagnostics**
  - Modern web frontend with real-time health tables, latency charts, incident triage view, and evidence viewer.
- **Weeks 11–12: Containerization, Testing & Placement Polish**
  - Complete integration test suite (JUnit 5, Mockito, Testcontainers).
  - Multi-container `docker-compose.yml` packaging.
  - Final project documentation, presentation slides, and demo script.

---

## 🚀 Quickstart & Setup

*(Detailed setup instructions will be provided as module code is committed.)*

### Prerequisites
- Java 21 JDK (or Java 17)
- Node.js 18+ (for frontend dashboard)
- Python 3.10+ (for ML service)
- MySQL 8.0+
- Docker & Docker Compose
