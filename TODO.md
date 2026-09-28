# IncidentPilot - Prioritized Task List (TODO)

This task list helps the team prioritize high-impact deliverables during the hackathon.

---

## Person 1: Backend / API Orchestration (Completed)
- [x] Set up core Spring Boot service.
- [x] Build incident submission endpoint (`POST /api/incidents/analyze`).
- [x] Build postmortem submission endpoint (`POST /api/postmortems`).
- [x] Wire request flow: incident -> Hindsight recall -> LLM call -> response.
- [x] Wire postmortem flow: postmortem -> Hindsight retain -> reflect.
- [x] Enable global CORS (`@CrossOrigin`) across all REST controllers for frontend connectivity.

---

## Person 2: Hindsight Memory Integration (Completed)
- [x] Stand up Hindsight integration (`HindsightClient.java`).
- [x] Design memory schema (service, symptoms, root cause, fix, failed approaches, prevention, lessons).
- [x] Implement and test recall queries with cloud fallback store.
- [x] Implement reflect consolidation for accumulated postmortems.

---

## Person 3: LLM Prompting & Diagnosis Logic Lead (Completed)
- [x] Design structured prompt combining CURRENT INCIDENT + HISTORICAL MEMORY -> structured diagnosis output.
- [x] Structure output format: root cause hypothesis, investigation steps, possible fixes, related past incidents, failed approaches to avoid, memory insights, confidence score.
- [x] Build "without memory" vs "with memory" comparison logic (`IncidentComparisonResponse` & `POST /api/incidents/compare` & `POST /api/demo/compare`).
- [x] Tune prompts and reasoning logic across 4 seed incident scenarios (Checkout connection leak, Auth JWKS cache, Order thread pool deadlock, Notification Kafka poison pill).
- [x] Enforce strict safety guardrails (no destructive commands, no remediation claims, no fabricated memory).
- [x] Add comprehensive test suite (`IncidentComparisonTest.java`, `LLMClientTest.java`, `DemoModeTest.java`, `QAScenariosTest.java`).

---

## Person 4: Frontend + Seed Data + Demo Lead (In Progress / Ready for UI)
- [x] Seed data preloaded (`POST /api/demo/seed-memory`, `POST /api/demo/seed-all`).
- [x] Demo flow scripts created (`docs/demo-flow.md` with Option A 1-Click Comparison and Option B 7-Step walkthrough).
- [ ] Connect React/web frontend UI to `http://localhost:8080`.
- [ ] Live demo rehearsal with presentation team.