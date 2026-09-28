# Development & Testing Guide

This guide covers setup, environment configuration, testing, and team development workflows for **IncidentPilot**.

---

## 1. Local Setup

### Prerequisites
- **JDK 21** or later (`java -version`)
- **Git**
- Maven (or use the included Maven wrapper `mvnw` / `mvnw.cmd`)

### Setup Steps
1. Clone the repository:
   ```bash
   git clone <repo-url>
   cd incidentpilot
   ```
2. Copy environment template:
   ```bash
   cp .env.example .env
   ```
3. Configure your API keys in `.env` or system environment if connecting to live providers. (Optional: the backend runs smoothly with in-memory store and dynamic reasoning fallback even if keys are left blank).

---

## 2. Running the Application

- **Windows**:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- **Linux / macOS**:
  ```bash
  ./mvnw spring-boot:run
  ```

The server starts on port `8080` (or the port defined by `SERVER_PORT`).

---

## 3. Automated Testing

The project uses JUnit 5 and Spring Boot Test.

- **Run all tests (Windows)**:
  ```powershell
  .\mvnw.cmd clean test
  ```
- **Run all tests (Linux / macOS)**:
  ```bash
  ./mvnw clean test
  ```

### Test Suite Structure
- `IncidentpilotApplicationTests.java`: Context boot verification.
- `IncidentPilotWorkflowsTest.java`: Core incident analysis & postmortem workflow tests.
- `LLMClientTest.java`: Tests prompt synthesis, fallback reasoning, and safety guardrails (no command execution).
- `DemoModeTest.java`: Verifies reset, memory seeding, and similar incident demo endpoint flows.
- `QAScenariosTest.java`: Automated 4-stage end-to-end verification of all demo scenarios.

---

## 4. Git Branching Strategy

For this 2-day hackathon, keep the branching strategy lightweight:

```
main (always stable and demo-ready)
  |
  +-- feature/backend
  +-- feature/frontend
  +-- feature/hindsight
  +-- feature/demo
```

1. Create a branch: `git checkout -b feature/<name>`
2. Make changes and verify tests pass: `./mvnw clean test`
3. Commit using conventional messages (`feat: ...`, `fix: ...`, `docs: ...`)
4. Push and create a Pull Request into `main`