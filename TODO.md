# IncidentPilot - Prioritized Task List (TODO)

This task list helps the team prioritize high-impact deliverables during the 2-day hackathon.

---

## P0 — Must Complete for Demo (Highest Priority)
- [x] Implement core Incident Analysis endpoint (`POST /api/incidents/analyze`).
- [x] Implement core Postmortem Learning endpoint (`POST /api/postmortems`).
- [x] Implement Hindsight `recall`, `retain`, and `reflect` clients.
- [x] Implement LLM integration with structured JSON and safety guardrails (no command execution).
- [x] Build hackathon Demo Mode endpoints (`POST /api/demo/*`).
- [x] Verify that absence of memory yields generic advice and does not hallucinate.
- [x] Verify that presence of memory prioritizes proven fixes and warns against failed approaches.
- [ ] Connect React frontend to Spring Boot backend APIs.
- [ ] Configure CORS in Spring Boot if frontend is served on a different port.
- [ ] Rehearse the 5-minute live demo flow (Zero memory -> Seed memory -> Contextual analysis).

---

## P1 — Important If Time Allows (High Impact)
- [ ] Connect official Hindsight production endpoint once credentials/docs are verified.
- [ ] Add loading indicators and visual diff comparisons in the React frontend.
- [ ] Implement an extra demo scenario (e.g. Auth Service TLS certificate expiry).
- [ ] Add copy-to-clipboard buttons for investigation steps and remediation advice.
- [ ] Polish error banners and validation messages in the UI.

---

## P2 — Future Improvements (Post-Hackathon / Future Scope)
- [ ] Direct telemetry integration with Prometheus, Datadog, or Grafana alerts.
- [ ] Real-time log streaming integration with Elasticsearch / Loki.
- [ ] ChatOps bot integration (Slack / Discord / Microsoft Teams).
- [ ] Kubernetes operator / sidecar for automated diagnostic bundle collection.
- [ ] Automated remediation execution with human-in-the-loop approval workflows.
- [ ] Long-term memory graph visualization showing interconnected incident relationships.