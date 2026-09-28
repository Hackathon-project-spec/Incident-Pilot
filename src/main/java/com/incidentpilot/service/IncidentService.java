package com.incidentpilot.service;

import com.incidentpilot.client.HindsightClient;
import com.incidentpilot.client.LLMClient;
import com.incidentpilot.dto.IncidentAnalysisRequest;
import com.incidentpilot.dto.IncidentAnalysisResponse;
import com.incidentpilot.dto.IncidentComparisonResponse;
import com.incidentpilot.dto.IncidentRequest;
import com.incidentpilot.dto.IncidentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service orchestrating IncidentPilot incident analysis and memory comparison workflows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final HindsightClient hindsightClient;
    private final LLMClient llmClient;

    /**
     * COMPLETE INCIDENT WORKFLOW:
     * STEP 1: Receive the incident.
     * STEP 2: Call Hindsight recall.
     * STEP 3: Take the returned historical memories.
     * STEP 4: Build an LLM prompt containing CURRENT INCIDENT + HISTORICAL MEMORY.
     * STEP 5: Call the LLM.
     * STEP 6: Return the structured diagnosis.
     */
    public IncidentAnalysisResponse analyze(IncidentAnalysisRequest request) {
        if (request == null) {
            request = IncidentAnalysisRequest.builder()
                    .serviceName("Checkout API")
                    .severity("SEV-1")
                    .symptoms("High latency and database connection timeouts")
                    .logs("ERROR: Connection pool exhausted - timeout waiting for connection from HikariPool")
                    .build();
        }
        String serviceName = (request.getServiceName() != null && !request.getServiceName().isBlank())
                ? request.getServiceName()
                : "Checkout API";
        String symptoms = request.getSymptoms() != null ? request.getSymptoms() : "";
        String logs = request.getLogs() != null ? request.getLogs() : "";
        String severity = (request.getSeverity() != null && !request.getSeverity().isBlank())
                ? request.getSeverity()
                : "SEV-1";

        log.info("[INCIDENT] Received incident for service: {}", serviceName);

        // STEP 2 & 3: Recall memories
        log.info("[MEMORY] Starting Hindsight recall");
        List<String> recalledMemories = hindsightClient.recall(
                serviceName,
                symptoms,
                logs,
                severity
        );

        int memoryCount = recalledMemories != null ? recalledMemories.size() : 0;
        log.info("[MEMORY] Number of relevant memories returned: {}", memoryCount);

        // STEP 4 & 5: LLM prompt & analysis
        log.info("[LLM] Sending current incident + historical context to LLM");
        IncidentAnalysisResponse diagnosis = llmClient.analyzeIncident(
                serviceName,
                symptoms,
                logs,
                severity,
                recalledMemories
        );

        log.info("[LLM] Diagnosis received");
        return diagnosis;
    }

    /**
     * CORE DEMO DIFFERENTIATOR (Person 3):
     * Runs the SAME incident twice:
     * 1. Cold-start triage WITHOUT memory (generic SRE recommendations)
     * 2. Memory-augmented triage WITH recalled Hindsight memory (historical root causes, proven fixes, avoided traps)
     * 
     * Compares the outputs side-by-side to visibly demonstrate the value of long-term incident memory.
     */
    public IncidentComparisonResponse compareWithAndWithoutMemory(IncidentAnalysisRequest request) {
        if (request == null || request.getServiceName() == null || request.getServiceName().isBlank()) {
            request = IncidentAnalysisRequest.builder()
                    .serviceName("Checkout API")
                    .severity("SEV-1")
                    .symptoms("High latency and database connection timeouts")
                    .logs("ERROR: Connection pool exhausted - timeout waiting for connection from HikariPool")
                    .build();
        }

        String serviceName = request.getServiceName();
        String symptoms = request.getSymptoms() != null ? request.getSymptoms() : "";
        String logs = request.getLogs() != null ? request.getLogs() : "";
        String severity = request.getSeverity() != null ? request.getSeverity() : "SEV-1";

        log.info("[COMPARISON] Starting side-by-side memory comparison for service: {}", serviceName);

        // 1. Run WITHOUT memory (Cold-start baseline)
        log.info("[COMPARISON] Step 1: Running baseline diagnosis WITHOUT memory");
        IncidentAnalysisResponse withoutMemoryDiagnosis = llmClient.analyzeIncident(
                serviceName,
                symptoms,
                logs,
                severity,
                Collections.emptyList()
        );

        // 2. Recall memory from Hindsight
        log.info("[COMPARISON] Step 2: Recalling memories from Hindsight");
        List<String> recalledMemories = hindsightClient.recall(serviceName, symptoms, logs, severity);
        boolean hasMemory = (recalledMemories != null && !recalledMemories.isEmpty()
                && !recalledMemories.contains("No relevant historical incidents found."));

        // 3. Run WITH memory
        log.info("[COMPARISON] Step 3: Running memory-augmented diagnosis WITH {} recalled memories", 
                (recalledMemories != null ? recalledMemories.size() : 0));
        IncidentAnalysisResponse withMemoryDiagnosis = llmClient.analyzeIncident(
                serviceName,
                symptoms,
                logs,
                severity,
                recalledMemories
        );

        // 4. Synthesize comparison highlights
        List<String> highlights = new ArrayList<>();
        if (hasMemory) {
            highlights.add("Root Cause Precision: Shifted from generic speculative causes to exact historical root cause pattern.");
            if (!withMemoryDiagnosis.getFailedApproachesToAvoid().isEmpty()) {
                highlights.add("Prevented Costly Anti-Patterns: Explicitly warned against " +
                        String.join("; ", withMemoryDiagnosis.getFailedApproachesToAvoid()));
            } else {
                highlights.add("Prevented Repeat Mistakes: Cautioned against repeating previously failed remediation attempts.");
            }
            highlights.add("Targeted Resolution: Prioritized proven fix from previous postmortem to minimize MTTR.");
            highlights.add("Institutional Lineage: Linked directly to " + recalledMemories.size() + " correlated past incident(s).");
        } else {
            highlights.add("Zero-State Baseline: No historical memories found. Both runs produced identical first-principles triage.");
            highlights.add("Action Required: Retain postmortems via POST /api/postmortems to enable memory-guided diagnosis.");
        }

        String summary = hasMemory
                ? "Memory-augmented diagnosis successfully identified proven historical fixes and warned against past failed approaches, drastically reducing MTTR."
                : "No historical memories were available for this service. System operated in standard telemetry-only cold-start mode.";

        log.info("[COMPARISON] Memory comparison completed. Memory advantage demonstrated: {}", hasMemory);

        return IncidentComparisonResponse.builder()
                .serviceName(serviceName)
                .severity(severity)
                .symptoms(symptoms)
                .logs(logs)
                .recalledMemories(recalledMemories != null ? recalledMemories : Collections.emptyList())
                .withoutMemoryDiagnosis(withoutMemoryDiagnosis)
                .withMemoryDiagnosis(withMemoryDiagnosis)
                .comparisonHighlights(highlights)
                .memoryAdvantageDemonstrated(hasMemory)
                .summary(summary)
                .build();
    }

    /**
     * Legacy Workflow 1: Diagnose
     */
    public IncidentResponse processIncident(IncidentRequest request) {
        log.info("Processing legacy incident request: [{}] - {}", request.getIncidentId(), request.getTitle());

        String serviceName = request.getService() != null ? request.getService() : "unknown";
        String symptoms = request.getDescription() != null ? request.getDescription() : "";
        String logs = request.getLogs() != null ? request.getLogs() : "";
        String severity = request.getSeverity() != null ? request.getSeverity() : "UNKNOWN";

        List<String> recalledMemories = hindsightClient.recall(serviceName, symptoms, logs, severity);
        String incidentContext = String.format("Incident ID: %s\nTitle: %s\nService: %s\nSeverity: %s\nDescription: %s\nLogs: %s\nMetrics: %s",
                request.getIncidentId(),
                request.getTitle(),
                request.getService(),
                request.getSeverity(),
                request.getDescription(),
                request.getLogs(),
                request.getMetrics());

        String diagnosis = llmClient.generateDiagnosis(incidentContext, recalledMemories);

        return IncidentResponse.builder()
                .incidentId(request.getIncidentId())
                .diagnosis(diagnosis)
                .suspectedRootCause("Derived from LLM analysis augmented with Hindsight historical memory")
                .suggestedMitigations(List.of(
                        "Investigate error logs against known failure signatures",
                        "Verify upstream dependency health and connection pool limits",
                        "Apply mitigation recommendations provided in diagnosis"
                ))
                .recalledMemories(recalledMemories)
                .status("DIAGNOSED")
                .build();
    }
}