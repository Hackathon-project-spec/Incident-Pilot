# Contributing to IncidentPilot

Welcome to the **IncidentPilot** repository! We are building an AI Incident Response Agent with Long-Term Memory during this hackathon. This guide outlines how team members can collaborate effectively.

---

## Team Workflow

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd incidentpilot
   ```

2. **Create a feature branch**
   Always branch off `main` for your designated feature area:
   ```bash
   git checkout -b feature/<feature-name>
   ```
   *Examples:*
   - `feature/frontend`
   - `feature/hindsight-integration`
   - `feature/demo-scenarios`
   - `feature/llm-prompt-tuning`

3. **Make your changes**
   - Keep changes focused on your component.
   - Do NOT commit `.env` or sensitive API keys.
   - Ensure you follow the project package conventions (`com.incidentpilot.*`).

4. **Run the test suite**
   Always verify that all tests compile and pass before pushing:
   - On Windows:
     ```powershell
     .\mvnw.cmd clean test
     ```
   - On macOS/Linux:
     ```bash
     ./mvnw clean test
     ```

5. **Commit your changes**
   Use clear, conventional commit messages:
   - `feat: add incident analysis endpoint`
   - `feat: integrate hindsight recall`
   - `feat: add postmortem workflow`
   - `fix: handle hindsight timeout`
   - `docs: update README`

   ```bash
   git add .
   git commit -m "feat: your feature summary"
   ```

6. **Push and create a Pull Request**
   ```bash
   git push origin feature/<feature-name>
   ```
   - Open a PR into `main`.
   - Have at least one other team member review the PR before merging.
   - Merge into `main` and pull updates locally (`git pull origin main`).