# Hackathon Demo Flow (5-Minute Walkthrough)

This guide documents the exact 7-step demonstration flow for hackathon judges and audiences.

---

## Prerequisites
Start the backend service:
```powershell
.\mvnw.cmd spring-boot:run
```
Service runs at `http://localhost:8080`.

---

## The 7-Step Demonstration Sequence

### STEP 1: Submit an incident with no relevant historical memory
Reset memory baseline and send an incident for a new service (`Billing API`):
```bash
curl -X POST http://localhost:8080/api/demo/reset

curl -X POST http://localhost:8080/api/incidents/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "Billing API",
    "symptoms": "HTTP 500 error rate spiked to 15%",
    "logs": "NullPointerException in InvoiceGenerator.java:142",
    "severity": "SEV-2"
  }'
```

### STEP 2: Show generic diagnosis
Point out to the audience:
- `historicalIncidents` shows: `["No relevant historical incidents found."]`.
- The system provides generic investigation steps based on telemetry without hallucinating or fabricating memories.

### STEP 3: Submit a postmortem
An engineer resolves an incident and logs what actually happened:
```bash
curl -X POST http://localhost:8080/api/postmortems \
  -H "Content-Type: application/json" \
  -d '{
    "incidentId": "INC-001",
    "serviceName": "Checkout API",
    "actualRootCause": "Database connection leak in unclosed transaction block",
    "successfulFix": "Corrected connection lifecycle handling",
    "failedApproaches": "Increasing database connection pool size",
    "preventionStrategy": "Added HikariCP connection leak detection",
    "lessonsLearned": "Check connection lifecycle before increasing pool capacity"
  }'
```

### STEP 4: Retain knowledge in Hindsight
Show the response:
- `retentionStatus: "SUCCESS"`
- `reflectionStatus: "SUCCESS"`
- `status: "COMPLETED"`
- Knowledge is now consolidated into long-term memory graph.

### STEP 5: Submit a similar incident
A new engineer encounters a similar issue on the Checkout API weeks later:
```bash
curl -X POST http://localhost:8080/api/incidents/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "Checkout API",
    "symptoms": "Latency increased from 200ms to 4 seconds, database connection timeouts",
    "logs": "ERROR: Connection pool exhausted - timeout waiting for connection from HikariPool",
    "severity": "SEV-1"
  }'
```

### STEP 6: Show that Hindsight recalls the previous incident
Inspect response:
- `historicalIncidents` lists `INC-001`.
- Recalls the exact root cause (`Database connection leak`).

### STEP 7: Show that the LLM produces a contextual recommendation
Highlight the key differentiator:
- `recommendedFixes`: `"Check corrected connection lifecycle handling before increasing database connection pool size (which previously failed in historical memory)."`
- `memoryBasedInsights`: Highlights that the system cautioned engineers against repeating the failed attempt (`Increasing connection pool size`) and prioritized the proven fix.