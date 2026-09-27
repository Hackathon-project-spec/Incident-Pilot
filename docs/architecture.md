# Architecture & Data Flow

This document details the internal architecture and runtime data flows of **IncidentPilot**.

---

## 1. System Overview

IncidentPilot is a Spring Boot application acting as an intelligent API orchestration layer between engineers, long-term memory (Hindsight), and reasoning models (LLM).

```
+-------------------------------------------------------------+
|                      React Frontend                         |
+-------------------------------------------------------------+
                              |
                              v [HTTP REST]
+-------------------------------------------------------------+
|                   Spring Boot Backend                       |
|                                                             |
|   +-----------------------+     +-----------------------+   |
|   |  IncidentController   |     | PostmortemController  |   |
|   +-----------------------+     +-----------------------+   |
|               |                             |               |
|               v                             v               |
|   +-----------------------+     +-----------------------+   |
|   |    IncidentService    |     |   PostmortemService   |   |
|   +-----------------------+     +-----------------------+   |
|         |           |                       |               |
|         v           |                       v               |
|   +-----------+     |                 +-----------+         |
|   | Hindsight |<----+-----------------| Hindsight |         |
|   |  Client   |                       |  Client   |         |
|   +-----------+                       +-----------+         |
|         |                                   |               |
|         | recall()                          | retain()      |
|         |                                   | reflect()     |
|         v                                   v               |
|   +-----------------------------------------------+         |
|   |                   Hindsight                   |         |
|   |             (Long-Term Incident Memory)       |         |
|   +-----------------------------------------------+         |
|         |                                                   |
|         | Recalled Memories                                 |
|         v                                                   |
|   +-----------+                                             |
|   |    LLM    |                                             |
|   |  Client   |                                             |
|   +-----------+                                             |
|         |                                                   |
|         v chat/completions (Structured JSON)                |
|   +-----------------------------------------------+         |
|   |                     LLM                       |         |
|   |           (Diagnostic Reasoning Model)        |         |
|   +-----------------------------------------------+         |
+-------------------------------------------------------------+
```

---

## 2. Workflows & Data Flow

### Workflow 1: Incident Analysis (`POST /api/incidents/analyze`)
1. **Receive Incident**: `IncidentController` receives `serviceName`, `symptoms`, `logs`, and `severity`.
2. **Recall Memories**: `IncidentService` calls `HindsightClient.recall(...)`.
3. **Memory Extraction**: Hindsight searches historical incidents for matching telemetry and root cause patterns. If none are found, an empty list `[]` is returned.
4. **Build Prompt**: `LLMClient` synthesizes the prompt:
   - `CURRENT INCIDENT`: service, symptoms, logs, severity.
   - `HISTORICAL MEMORY`: previous similar incidents, successful fixes, failed approaches, prevention lessons. If none found: `"No relevant historical memory was found."`.
5. **LLM Query**: Queries the model using strict safety guardrails.
6. **Structured Response**: Returns possible root causes, investigation steps, recommended fixes, historical incidents, and memory-based insights.

### Workflow 2: Postmortem Learning (`POST /api/postmortems`)
1. **Receive Postmortem**: `PostmortemController` receives the resolved incident record:
   - `incidentId`, `serviceName`, `actualRootCause`, `successfulFix`, `failedApproaches`, `preventionStrategy`, `lessonsLearned`.
2. **Hindsight Retain**: `PostmortemService` calls `HindsightClient.retain(...)` to store learnings into the memory graph.
3. **Hindsight Reflect**: Calls `HindsightClient.reflect(...)` to consolidate patterns and relationships.
4. **Immediate Availability**: Any subsequent incident analysis on this service immediately recalls these learnings.

---

## 3. Strict Safety & Prototype Rules

- **No Command Execution**: The LLM prompt and client contract strictly forbid executing shell commands, terminal actions, or code execution.
- **No False Remediation Claims**: The LLM cannot claim or state that it fixed or remediated an issue; all outputs are advisory.
- **Unfabricated Memory**: When no prior incident exists, the system honestly states `"No relevant historical incidents found."` and never invents fake incidents.
- **Graceful Degraded Mode**: If live API keys are not supplied or network drops, the application falls back to dynamic local reasoning from memory without crashing.