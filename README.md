# IncidentPilot

### AI Incident Response Agent with Long-Term Memory

---

## 1. Overview

**IncidentPilot** is an AI-powered incident-response assistant built for backend, DevOps, and Site Reliability Engineering (SRE) teams.

Instead of diagnosing issues purely in isolation, IncidentPilot integrates **Hindsight** as a long-term memory engine. It recalls previous production incidents, their underlying root causes, the fixes that succeeded, the approaches that failed, and the preventive lessons learned by the team.

---

## 2. Problem

During high-severity production incidents, engineering teams lose critical time searching through scattered postmortems, old Slack threads, wikis, and log archives to find out if this issue occurred before.

Standard AI assistants can summarize current error logs, but they have no awareness of a team’s past experiences, previous postmortems, or organization-specific architectural quirks. As a result, engineers frequently repeat the same ineffective or counter-productive troubleshooting steps.

---

## 3. Solution

IncidentPilot solves this by bridging real-time observability with long-term organizational memory. It combines:

$$\text{Current Incident Telemetry} + \text{Hindsight Historical Memory} + \text{LLM Diagnostic Reasoning}$$

When an incident strikes, IncidentPilot automatically recalls relevant past incidents and guides engineers directly to verified solutions while explicitly cautioning against failed approaches.

---

## 4. Key Differentiator

> **"IncidentPilot doesn't just remember what happened. It remembers what actually worked and what didn't."**

Most tools stop at semantic search or log indexing. IncidentPilot pairs historical root causes with both **successful fixes** and **failed approaches**, ensuring engineers never repeat past mistakes during critical outages.

---

## 5. MVP Workflow

### Workflow 1: Incident Analysis
```
Incident Occurs
      ↓
Hindsight Recall (Queries past incidents for matching patterns)
      ↓
LLM Analysis (CURRENT INCIDENT + HISTORICAL MEMORY)
      ↓
Structured Diagnosis (Root causes, fixes, warnings, memory insights)
```

### Workflow 2: Postmortem Learning
```
Postmortem Submitted (Root cause, fix, failed approaches, lessons)
      ↓
Hindsight Retain (Stores learnings into long-term memory)
      ↓
Hindsight Reflect (Consolidates memory graph)
      ↓
Long-Term Incident Memory (Instantly available for future incidents)
```

---

## 6. Architecture

```
React Frontend
      |
      v
Spring Boot Backend
      |
      +----> Hindsight (Long-Term Memory)
      |        |
      |        +--> Recall   (Retrieve similar incidents)
      |        +--> Retain   (Store postmortem learnings)
      |        +--> Reflect  (Consolidate memory graph)
      |
      +----> LLM Provider (Diagnostic Reasoning)
               |
               v
          Structured Diagnosis
```

---

## 7. Tech Stack

- **Java 21**: Core language runtime.
- **Spring Boot 4.x**: Backend orchestration and REST API layer.
- **Maven**: Build and dependency management (includes Maven Wrapper `mvnw` / `mvnw.cmd`).
- **Spring Web**: RESTful MVC web tier.
- **WebClient (Spring WebFlux)**: Non-blocking HTTP client for Hindsight and LLM communications.
- **Lombok**: Boilerplate reduction for data models and DTOs.
- **Hindsight**: Long-term memory storage, recall, and reflection service.
- **LLM API**: Diagnostic reasoning model (OpenAI-compatible / Gemini proxy).
- **React**: Frontend UI (planned / connected during hackathon).

---

## 8. Current Backend Structure

```
src/main/java/com/incidentpilot/
├── controller/        # REST controllers handling HTTP requests and responses
│   ├── IncidentController.java       # POST /api/incidents/analyze & /diagnose
│   ├── PostmortemController.java     # POST /api/postmortems
│   └── DemoController.java           # POST /api/demo/* endpoints for judging
├── service/           # Business logic orchestrating Hindsight and LLM
│   ├── IncidentService.java          # 6-step incident analysis pipeline
│   └── PostmortemService.java        # Retain and reflect postmortem lifecycle
├── client/            # HTTP clients for outbound service communications
│   ├── HindsightClient.java          # Recall, retain, and reflect operations
│   └── LLMClient.java                # Prompt generation, JSON mode, fallback reasoning
├── dto/               # Data Transfer Objects for API request/response contracts
│   ├── IncidentAnalysisRequest.java
│   ├── IncidentAnalysisResponse.java
│   ├── PostmortemRequest.java
│   ├── PostmortemResponse.java
│   ├── IncidentRequest.java          # Legacy compatibility
│   └── IncidentResponse.java         # Legacy compatibility
├── config/            # Infrastructure beans and configuration
│   └── WebClientConfig.java          # Outbound WebClient builder bean
└── IncidentpilotApplication.java     # Spring Boot application entrypoint
```

---

## 9. API Endpoints

### 1. Analyze Incident
- **URL**: `POST /api/incidents/analyze`
- **Purpose**: Analyzes an incident against historical memory and generates contextual recommendations.
- **Request**:
  ```json
  {
    "serviceName": "Checkout API",
    "symptoms": "Latency increased from 200ms to 4 seconds",
    "logs": "Database connection timeout errors",
    "severity": "SEV-1"
  }
  ```
- **Response**:
  ```json
  {
    "serviceName": "Checkout API",
    "severity": "SEV-1",
    "possibleRootCauses": ["Historical pattern: Database connection leak"],
    "investigationSteps": ["Inspect database slow query log and active lock waits"],
    "recommendedFixes": [
      "Check corrected connection lifecycle handling before increasing database connection pool size (which previously failed in historical memory)."
    ],
    "historicalIncidents": ["Previous Checkout API incident (INC-001) was caused by: Database connection leak."],
    "memoryBasedInsights": [
      "Historical memory directly influenced recommendation: Prioritizing proven fix and cautioning against previously failed approach."
    ]
  }
  ```

### 2. Submit Postmortem
- **URL**: `POST /api/postmortems`
- **Purpose**: Stores resolved incident knowledge into Hindsight long-term memory.
- **Request**:
  ```json
  {
    "incidentId": "INC-001",
    "serviceName": "Checkout API",
    "actualRootCause": "Database connection leak",
    "successfulFix": "Corrected connection lifecycle handling",
    "failedApproaches": "Increasing database connection pool size",
    "preventionStrategy": "Added connection monitoring",
    "lessonsLearned": "Check connection lifecycle before increasing pool capacity"
  }
  ```
- **Response**:
  ```json
  {
    "incidentId": "INC-001",
    "serviceName": "Checkout API",
    "retentionStatus": "SUCCESS",
    "reflectionStatus": "SUCCESS",
    "status": "COMPLETED",
    "message": "Postmortem successfully retained into long-term memory and consolidated via reflection."
  }
  ```

### 3. Demo Endpoints (Hackathon Prototype Only)
- `POST /api/demo/reset`: Resets memory store to a clean baseline.
- `POST /api/demo/seed-memory`: Seeds canonical historical incident memory via the official retain flow.
- `POST /api/demo/similar-incident`: Analyzes a similar incident to demonstrate dynamic memory recall.

---

## 10. Example Incident

```
Service:   Checkout API
Symptoms:  Latency increased from 200ms to 4 seconds.
Logs:      Database connection timeout errors.
Severity:  SEV-1
```

**How IncidentPilot processes it:**
1. Receives the incident telemetry.
2. Queries Hindsight for incidents related to `Checkout API` with connection/database anomalies.
3. Retrieves past postmortems including the previous root cause and fix.
4. Synthesizes a prompt providing the LLM with the active symptoms plus the past learnings.
5. Emits a structured recommendation advising engineers on what worked and warning against repeating what failed.

---

## 11. Example Historical Memory

```
Previous Incident:   Checkout API Database Outage
Previous Root Cause: Database connection leak
Successful Fix:      Corrected connection lifecycle handling
Failed Approach:     Increasing database connection pool size
```

**Influence on Diagnosis:**  
Instead of generically suggesting *"increase your database connection pool size"* (a common AI hallucination that previously failed), IncidentPilot specifically warns:
> *"Check corrected connection lifecycle handling before increasing database connection pool size (which previously failed in historical memory)."*

---

## 12. Example Postmortem Request

Sample JSON based on the actual `PostmortemRequest` model:

```json
{
  "incidentId": "INC-101",
  "serviceName": "Payment Gateway",
  "symptoms": "Thread pool exhaustion and socket timeouts",
  "actualRootCause": "Downstream bank endpoint latency causing hung threads",
  "successfulFix": "Enabled circuit breaker and reduced socket connect timeout",
  "failedApproaches": "Retrying 10 times immediately",
  "preventionStrategy": "Synthetic canary health checks",
  "lessonsLearned": "Fail fast instead of allowing cascading thread exhaustion"
}
```

---

## 13. Setup Requirements

- **JDK 21** or later installed.
- **Git** for version control.
- **Maven** or the included Maven wrapper (`mvnw` / `mvnw.cmd`).
- *(Optional)* Hindsight API credentials.
- *(Optional)* LLM API credentials (e.g. OpenAI API key).

> [!NOTE]
> The backend runs fully out-of-the-box in standalone demo mode with built-in memory retention and local dynamic SRE reasoning, even if live API keys are not supplied.

---

## 14. Environment Configuration

1. Clone the repository:
   ```bash
   git clone <repo-url>
   cd incidentpilot
   ```
2. Copy the template to `.env`:
   ```bash
   cp .env.example .env
   ```
3. Set your environment variables (or export them in your terminal):
   ```bash
   SERVER_PORT=8080
   LLM_API_KEY=your-api-key-here
   LLM_BASE_URL=https://api.openai.com/v1
   LLM_MODEL=gpt-4o-mini
   HINDSIGHT_API_KEY=your-hindsight-key
   HINDSIGHT_BASE_URL=https://api.hindsight.ai
   ```

---

## 15. Running the Backend

### Windows
```powershell
# Run tests
mvnw.cmd clean test

# Start the application
mvnw.cmd spring-boot:run
```

### Linux / macOS
```bash
# Run tests
./mvnw clean test

# Start the application
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

---

## 16. Testing

The repository includes a comprehensive, fast automated test suite:

- `IncidentpilotApplicationTests.java`: Validates application context initialization.
- `IncidentPilotWorkflowsTest.java`: Verifies full incident diagnosis and postmortem retain/reflect pipelines.
- `LLMClientTest.java`: Verifies prompt construction, memory formatting, fallback reasoning, and safety guardrails.
- `DemoModeTest.java`: Verifies clean reset, demo memory seeding, and memory-influenced analysis.
- `QAScenariosTest.java`: End-to-end verification of all 4 hackathon presentation scenarios.

Run all tests:
```powershell
mvnw.cmd clean test
```

---

## 17. Demo Flow

The 5-minute hackathon presentation sequence:

1. **STEP 1**: Submit an incident with no relevant historical memory (`Billing API`).
2. **STEP 2**: Show the generic diagnosis clearly stating that no relevant historical memory was found.
3. **STEP 3**: Submit a postmortem resolving a previous `Checkout API` incident.
4. **STEP 4**: Show Hindsight retaining and reflecting upon the postmortem.
5. **STEP 5**: Submit a new, similar incident on `Checkout API`.
6. **STEP 6**: Show that Hindsight immediately recalls the previous incident.
7. **STEP 7**: Show that the LLM produces a contextual recommendation prioritizing the proven fix and cautioning against the previously failed approach.

---

## 18. Team Development

Suggested team responsibilities for 4 members during the hackathon:

- **Person 1 (Backend / API Orchestration)**:
  - Incident & Postmortem endpoints, controllers, DTOs, service integration, and CORS configuration.
- **Person 2 (Frontend)**:
  - React UI, incident input form, diagnosis display, postmortem intake, and demo trigger buttons.
- **Person 3 (Hindsight & AI)**:
  - Hindsight memory schema, live API connectivity, prompt engineering, and LLM output parsing.
- **Person 4 (Demo, Integration & Testing)**:
  - End-to-end verification, demo data scripts, presentation slides, and live judging rehearsal.

*(These roles are flexible suggestions and can be adapted by the team as needed.)*

---

## 19. Git Workflow

```
main (always stable and demo-ready)
  |
  +-- feature/backend
  +-- feature/frontend
  +-- feature/hindsight
  +-- feature/demo
```

Recommended steps:
1. `git clone <repo-url>`
2. `git checkout -b feature/<name>`
3. Make changes and verify tests pass (`./mvnw clean test`)
4. `git add .`
5. `git commit -m "feat: your change summary"`
6. `git push origin feature/<name>`
7. Create a Pull Request and merge to `main` after review.

---

## 20. Future Scope

The following features represent post-hackathon enhancements and are **NOT** part of the current MVP:

- Real-time Prometheus and Datadog alert ingestion.
- Automated log scraping from Elasticsearch / Grafana Loki.
- Slack / Microsoft Teams incident response bot.
- Kubernetes operator for diagnostic bundle collection.
- Automated incident correlation across distributed microservices.
- Automated remediation execution with human-in-the-loop approval gates.

---

## 21. Security

- **No Secrets in Git**: Never commit `.env` or hardcode API keys into Java or properties files.
- **Use `.env.example`**: Store placeholders only in version-controlled configuration templates.
- **No Command Execution**: IncidentPilot is designed strictly as an advisory system and does not execute infrastructure commands.

---

## 22. License

This project is licensed under the [MIT License](LICENSE).