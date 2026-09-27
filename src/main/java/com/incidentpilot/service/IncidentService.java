package com.incidentpilot.service;

import com.incidentpilot.client.HindsightClient;
import com.incidentpilot.client.LLMClient;
import com.incidentpilot.dto.IncidentAnalysisRequest;
import com.incidentpilot.dto.IncidentAnalysisResponse;
import com.incidentpilot.dto.IncidentRequest;
import com.incidentpilot.dto.IncidentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service orchestrating the complete IncidentPilot incident analysis workflow.
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
                    .serviceName("unknown")
                    .severity("SEV-3")
                    .symptoms("Unspecified incident symptoms")
                    .logs("")
                    .build();
        }
        String serviceName = (request.getServiceName() != null && !request.getServiceName().isBlank())
                ? request.getServiceName()
                : "unknown";
        String symptoms = request.getSymptoms() != null ? request.getSymptoms() : "";
        String logs = request.getLogs() != null ? request.getLogs() : "";
        String severity = (request.getSeverity() != null && !request.getSeverity().isBlank())
                ? request.getSeverity()
                : "SEV-3";

        // [INCIDENT] Received incident for service: <serviceName>
        log.info("[INCIDENT] Received incident for service: {}", serviceName);

        // [MEMORY] Starting Hindsight recall
        log.info("[MEMORY] Starting Hindsight recall");
        List<String> recalledMemories = hindsightClient.recall(
                serviceName,
                symptoms,
                logs,
                severity
        );

        // [MEMORY] Number of relevant memories returned: X
        int memoryCount = recalledMemories != null ? recalledMemories.size() : 0;
        log.info("[MEMORY] Number of relevant memories returned: {}", memoryCount);

        // [LLM] Sending current incident + historical context to LLM
        log.info("[LLM] Sending current incident + historical context to LLM");
        IncidentAnalysisResponse diagnosis = llmClient.analyzeIncident(
                serviceName,
                symptoms,
                logs,
                severity,
                recalledMemories
        );

        // [LLM] Diagnosis received
        log.info("[LLM] Diagnosis received");
        return diagnosis;
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