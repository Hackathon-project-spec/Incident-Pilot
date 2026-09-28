# Hackathon Demo Flow (5-Minute Walkthrough)

This guide documents the exact step-by-step demonstration flow for hackathon presentations, judging, and live walkthroughs.

---

## Prerequisites
Start the backend service:
```powershell
.\mvnw.cmd spring-boot:run
```
Service runs at `http://localhost:8080`.

---

## The Core Demonstration Sequences

### Option A: The Live 1-Click Comparison Demo (Fastest & Highest Impact)

1. **Seed Historical Knowledge**:
   ```bash
   curl -X POST http://localhost:8080/api/demo/seed-memory
   ```
2. **Run Side-by-Side Comparison**:
   ```bash
   curl -X POST http://localhost:8080/api/demo/compare
   ```
   *(Or simply open `http://localhost:8080/api/demo/compare` in the browser)*

**What to highlight to judges**:
- **Without Memory**: Generically suggests increasing the pool size or checking database connections.
- **With Memory**: Pinpoints the exact historical root cause (`Database connection leak`), recommends the proven fix (`try-with-resources`), and **explicitly warns against increasing pool size** because it previously failed.
- **Comparison Highlights**: Visually proves the MTTR reduction and elimination of repeated mistakes.

---

### Option B: The Complete 7-Step Step-by-Step Story

#### STEP 1: Submit an incident with no relevant historical memory
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

#### STEP 2: Show generic diagnosis
Point out to the audience:
- `historicalIncidents` shows: `["No relevant historical incidents found."]`.
- The system provides generic first-principles investigation steps based solely on telemetry without fabricating past incidents.

#### STEP 3: Submit a postmortem
An engineer resolves an incident and logs what actually happened:
```bash
curl -X POST http://localhost:8080/api/postmortems \
  -H "Content-Type: application/json" \
  -d '{
    "incidentId": "INC-001",
    "serviceName": "Checkout API",
    "symptoms": "High latency and database connection timeouts",
    "actualRootCause": "Database connection leak in payment processing loop",
    "successfulFix": "Corrected connection lifecycle handling using try-with-resources",
    "failedApproaches": "Increasing database connection pool size",
    "preventionStrategy": "Added HikariCP connection leak detection threshold monitoring",
    "lessonsLearned": "Check connection lifecycle before increasing pool capacity"
  }'
```

#### STEP 4: Retain knowledge in Hindsight
Show the response:
- `retentionStatus: "SUCCESS"`
- `reflectionStatus: "SUCCESS"`
- `status: "COMPLETED"`
- Knowledge is now consolidated into long-term memory graph.

#### STEP 5: Submit a similar incident
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

#### STEP 6: Show that Hindsight recalls the previous incident
Inspect response:
- `historicalIncidents` lists `INC-001`.
- Recalls the exact root cause (`Database connection leak`).

#### STEP 7: Show that the LLM produces a contextual recommendation
Highlight the key differentiators:
- `recommendedFixes`: Prioritizes `try-with-resources` lifecycle fix before increasing pool size.
- `failedApproachesToAvoid`: Explicitly cautions against increasing database pool size.
- `memoryBasedInsights`: Highlights that the system prevented repeating the past failed approach.