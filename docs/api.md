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
      "Historical pattern: Database connection leak in unclosed transaction block"
    ],
    "investigationSteps": [
      "Check active versus idle connection pool metrics (e.g. HikariCP pool usage)",
      "Inspect database slow query log and active lock waits",
      "Verify network connectivity and latency between Checkout API and database cluster"
    ],
    "recommendedFixes": [
      "Check corrected connection lifecycle handling before increasing database connection pool size (which previously failed in historical memory)."
    ],
    "historicalIncidents": [
      "Previous Checkout API incident (INC-001) was caused by: Database connection leak in unclosed transaction block. Previous successful fix: Corrected connection lifecycle handling. Previous failed approach: Increasing database connection pool size. Lessons learned: Check connection lifecycle handling before increasing pool capacity."
    ],
    "memoryBasedInsights": [
      "Historical memory directly influenced recommendation: Prioritizing proven fix ('Corrected connection lifecycle handling') and cautioning against previously failed approach ('Increasing database connection pool size').",
      "Historical lesson applied: Check connection lifecycle handling before increasing pool capacity."
    ]
  }
  ```

---

## 2. Postmortem Learning API

### `POST /api/postmortems`
Stores a resolved incident postmortem into Hindsight long-term memory and triggers reflection consolidation.

- **Request Body**:
  ```json
  {
    "incidentId": "INC-001",
    "serviceName": "Checkout API",
    "symptoms": "High latency and database connection timeouts",
    "actualRootCause": "Database connection leak in unclosed transaction block",
    "successfulFix": "Corrected connection lifecycle handling and ensured session closure",
    "failedApproaches": "Increasing database connection pool size",
    "preventionStrategy": "Added HikariCP connection leak detection threshold monitoring",
    "lessonsLearned": "Check connection lifecycle handling before increasing pool capacity"
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

## 3. Demo APIs (Hackathon Prototype Only)

### `POST /api/demo/reset`
Clears in-memory prototype memory store to return to a clean zero-memory baseline.
- **Request Body**: None
- **Response Body (200 OK)**:
  ```json
  {
    "status": "RESET_SUCCESSFUL",
    "message": "Demo memory reset successfully. IncidentPilot is now in a clean zero-memory state."
  }
  ```

### `POST /api/demo/seed-memory`
Seeds standard historical incident memory using the official postmortem retain flow.
- **Request Body**: None
- **Response Body (200 OK)**: `PostmortemResponse` for `INC-DEMO-001`.

### `POST /api/demo/similar-incident`
Submits a similar Checkout API incident to showcase memory-guided reasoning.
- **Request Body**: Optional custom `IncidentAnalysisRequest` (defaults to Checkout API telemetry).
- **Response Body (200 OK)**: `IncidentAnalysisResponse`.

---

## 4. Legacy Diagnostic API

### `POST /api/incidents/diagnose`
Compatibility endpoint for legacy incident diagnose requests.
- **Request Body**: `IncidentRequest`
- **Response Body (200 OK)**: `IncidentResponse`