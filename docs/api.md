# API Documentation

This document covers all active REST endpoints in **IncidentPilot**.

---

## 1. Incident Analysis API

### `POST /api/incidents/analyze`
Analyzes an active production incident by recalling past incident memory from Hindsight and querying the LLM for contextual root causes and remediations.

- **Request Body**:
  ```json
  {
    "serviceName": "Checkout API",
    "symptoms": "Latency increased from 200ms to 4 seconds, database connection timeouts",
    "logs": "ERROR: Connection pool exhausted - timeout waiting for connection from HikariPool",
    "severity": "SEV-1"
  }
  ```

- **Response Body (200 OK)**:
  ```json
  {
    "serviceName": "Checkout API",
    "severity": "SEV-1",
    "possibleRootCauses": [
      "Historical pattern: Database connection leak in payment processing loop"
    ],
    "investigationSteps": [
      "Check active versus idle connection pool metrics (e.g. HikariCP active connections / wait time)",
      "Inspect database slow query log and active lock waits",
      "Verify connection lifecycle handling in application database access layer for unclosed connections"
    ],
    "recommendedFixes": [
      "Check corrected connection lifecycle handling using try-with-resources before increasing database connection pool size (hikaricp maxpoolsize) (which previously failed in historical memory)."
    ],
    "historicalIncidents": [
      "Previous Checkout API incident (INC-DEMO-001) was caused by: Database connection leak in payment processing loop. Previous successful fix: Corrected connection lifecycle handling using try-with-resources. Previous failed approach: Increasing database connection pool size (HikariCP maxPoolSize). Lessons learned: Check connection lifecycle before increasing pool capacity."
    ],
    "memoryBasedInsights": [
      "Historical memory directly influenced recommendation: Prioritizing proven fix ('Corrected connection lifecycle handling using try-with-resources') and cautioning against previously failed approach ('Increasing database connection pool size (HikariCP maxPoolSize)').",
      "Historical lesson applied: Check connection lifecycle before increasing pool capacity"
    ],
    "failedApproachesToAvoid": [
      "Avoid: Increasing database connection pool size (HikariCP maxPoolSize) (recorded as an ineffective/counter-productive approach in past incident)"
    ],
    "confidenceScore": "HIGH (Memory-Correlated)"
  }
  ```

---

## 2. "Without Memory" vs "With Memory" Comparison API (Demo Differentiator)

### `POST /api/incidents/compare` (also `GET /api/incidents/compare`)
Runs the same incident twice:
1. **Cold-start baseline** (without any historical memory)
2. **Memory-augmented analysis** (with recalled Hindsight memory)
Returns a side-by-side comparison illustrating the concrete value of memory.

- **Request Body** (optional):
  ```json
  {
    "serviceName": "Checkout API",
    "symptoms": "High latency and database connection timeouts",
    "logs": "ERROR: Connection pool exhausted - timeout waiting for connection from HikariPool",
    "severity": "SEV-1"
  }
  ```

- **Response Body (200 OK)**:
  See [`docs/api-examples/comparison-response.json`](api-examples/comparison-response.json) for full payload.

---

## 3. Postmortem Learning API

### `POST /api/postmortems`
Stores a resolved incident postmortem into Hindsight long-term memory and triggers reflection consolidation.

- **Request Body**:
  ```json
  {
    "incidentId": "INC-001",
    "serviceName": "Checkout API",
    "symptoms": "High latency and database connection timeouts",
    "actualRootCause": "Database connection leak in unclosed transaction block",
    "successfulFix": "Corrected connection lifecycle handling using try-with-resources",
    "failedApproaches": "Increasing database connection pool size",
    "preventionStrategy": "Added HikariCP connection leak detection threshold monitoring",
    "lessonsLearned": "Check connection lifecycle before increasing pool capacity"
  }
  ```

- **Response Body (200 OK)**:
  ```json
  {
    "incidentId": "INC-001",
    "serviceName": "Checkout API",
    "retentionStatus": "SUCCESS",
    "retentionId": "hindsight_mem_inc-001_1790541284616",
    "reflectionStatus": "SUCCESS",
    "reflectionSummary": "Reflection completed: Pattern consolidated into Hindsight memory graph.",
    "status": "COMPLETED",
    "message": "Postmortem successfully retained into long-term memory and consolidated via reflection."
  }
  ```

---

## 4. Demo APIs (Hackathon Prototype Only)

### `POST /api/demo/reset`
Clears in-memory prototype memory store to return to a clean zero-memory baseline.

### `POST /api/demo/seed-memory`
Seeds the standard canonical Checkout API incident memory through the official retain flow.

### `POST /api/demo/seed-all`
Preloads 4 realistic incident postmortems into Hindsight across multiple architectures:
1. **Checkout API** (Database connection leak vs pool resizing)
2. **Auth Service** (JWKS token rotation cache mismatch vs pod restarts)
3. **Order Processing Service** (Blocking sync HTTP calls in thread pool vs doubling pool)
4. **Notification Service** (Kafka poison pill message deserialization vs adding consumers)

### `POST /api/demo/compare` (also `GET /api/demo/compare`)
Executes the side-by-side memory comparison on demo incident data.

### `POST /api/demo/similar-incident`
Submits a similar Checkout API incident to showcase memory-guided reasoning.

---

## 5. Legacy Diagnostic API

### `POST /api/incidents/diagnose`
Compatibility endpoint for legacy incident diagnose requests.
- **Request Body**: `IncidentRequest`
- **Response Body (200 OK)**: `IncidentResponse`