# IncidentPilot - Team Handoff Document

Welcome team! This document gives you an immediate picture of where the project stands, what is working, what remains to be done, and where each team member should start.

---

## 1. Current Status

The backend prototype is **fully functional, stable, and tested** (13/13 passing tests). It can run standalone with in-memory memory consolidation and local reasoning fallbacks, or connected to live Hindsight and OpenAI-compatible LLM endpoints.

---

## 2. Backend Overview

### Active Endpoints
1. `POST /api/incidents/analyze`: Main incident analysis orchestration (Receive -> Recall -> LLM -> Diagnosis).
2. `POST /api/postmortems`: Postmortem ingestion (Retain -> Reflect -> Long-term memory).
3. `POST /api/demo/reset`: Resets demo in-memory memory store to zero state.
4. `POST /api/demo/seed-memory`: Seeds canonical Checkout API postmortem through the real retain flow.
5. `POST /api/demo/similar-incident`: Triggers similar incident analysis against seeded memory.
6. `POST /api/incidents/diagnose`: Legacy diagnose compatibility.

---

## 3. Hindsight Integration Status

- **What works now**:
  - `HindsightClient.java` contains full WebClient logic for `recall`, `retain`, and `reflect`.
  - When endpoint URLs are not yet configured, it uses a thread-safe `inMemoryStore` that mimics live memory retention and reflection for hackathon presentations.
- **What needs to be configured / connected**:
  - Once official Hindsight endpoint paths are finalized, update `.env` or `application.properties`:
    - `HINDSIGHT_API_KEY`: API key.
    - `HINDSIGHT_ENDPOINT_RECALL`: URI path (e.g. `/api/v1/memory/recall`).
    - `HINDSIGHT_ENDPOINT_RETAIN`: URI path (e.g. `/api/v1/memory/retain`).
    - `HINDSIGHT_ENDPOINT_REFLECT`: URI path (e.g. `/api/v1/memory/reflect`).

---

## 4. LLM Integration Status

- **What works now**:
  - `LLMClient.java` sends structured system prompts and user prompts with `CURRENT INCIDENT` + `HISTORICAL MEMORY`.
  - Configured for JSON mode (`response_format: {"type": "json_object"}`).
  - If `LLM_API_KEY` is empty or if the network fails during a demo, it automatically falls back to dynamic local SRE reasoning from actual recalled memory without throwing errors.
- **What needs configuration**:
  - Provide `LLM_API_KEY` in `.env` if using live OpenAI / Gemini proxy.
  - Set `LLM_MODEL` (defaults to `gpt-4o-mini`).

---

## 5. Frontend Team Guide

The frontend team can consume the backend running on `http://localhost:8080`.

- **Primary Flows to Build in UI**:
  1. **Incident Intake & Diagnosis View**:
     - Form: `serviceName`, `symptoms`, `logs`, `severity` (SEV-1, SEV-2, SEV-3).
     - Submit to: `POST /api/incidents/analyze`.
     - Display: `possibleRootCauses`, `investigationSteps`, `recommendedFixes`, `historicalIncidents`, and `memoryBasedInsights`.
  2. **Postmortem Ingestion Form**:
     - Form: `incidentId`, `serviceName`, `actualRootCause`, `successfulFix`, `failedApproaches`, `preventionStrategy`, `lessonsLearned`.
     - Submit to: `POST /api/postmortems`.
     - Display confirmation: `retentionStatus`, `reflectionStatus`, `message`.
  3. **Demo Quick Buttons**:
     - "Reset Memory" (`POST /api/demo/reset`)
     - "Seed Demo Data" (`POST /api/demo/seed-memory`)
     - "Run Similar Incident" (`POST /api/demo/similar-incident`)

*Check `docs/api-examples/` for sample payloads.*

---

## 6. Known TODOs & Unfinished Work

1. **CORS Configuration**: If the React frontend runs on `http://localhost:3000` or `5173`, add `@CrossOrigin` or a global CORS filter in Spring Boot.
2. **Official Hindsight API Spec**: Connect the exact REST paths for Hindsight once documentation credentials are provided.
3. **Frontend UI**: Currently, interactions are done via `curl` / Postman / tests; React UI needs to be developed and connected.
4. **Enhanced Telemetry Formatting**: Support pasting raw JSON metrics or stack traces cleanly in the frontend input.

---

## 7. "Where Should I Start?"

### Person 1: Backend / API Orchestration
- Start by checking `IncidentController.java`, `PostmortemController.java`, and `WebClientConfig.java`.
- Add CORS configuration if the React frontend connects from another port.
- Polish any additional telemetry fields requested by the frontend team.

### Person 2: Frontend (React)
- Create the React app (Vite + Tailwind or your preferred stack).
- Implement the Incident Form and Postmortem Form.
- Connect forms to `http://localhost:8080/api/incidents/analyze` and `http://localhost:8080/api/postmortems`.

### Person 3: Hindsight & AI Engineer
- Inspect `HindsightClient.java` and `LLMClient.java`.
- If you have official Hindsight API credentials, test them against the live endpoint.
- Tune the LLM system prompt in `LLMClient.java` for tone and formatting.

### Person 4: Demo, Integration & Testing
- Read `docs/demo-flow.md`.
- Run the demo script using `curl` or Postman.
- Prepare the slide deck or talk track comparing "Incident Analysis WITHOUT memory" vs "Incident Analysis WITH memory".