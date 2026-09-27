package com.incidentpilot.controller;

import com.incidentpilot.client.HindsightClient;
import com.incidentpilot.dto.IncidentAnalysisRequest;
import com.incidentpilot.dto.IncidentAnalysisResponse;
import com.incidentpilot.dto.PostmortemRequest;
import com.incidentpilot.dto.PostmortemResponse;
import com.incidentpilot.service.IncidentService;
import com.incidentpilot.service.PostmortemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ============================================================================
 * HACKATHON PROTOTYPE ONLY - DEMO MODE CONTROLLER
 * ============================================================================
 * These endpoints are strictly intended for demonstrating the AI long-term
 * memory concept reliably during hackathon presentations and judging.
 * 
 * Flow for demo:
 * 1. POST /api/demo/reset: Reset memory to a clean state.
 * 2. POST /api/demo/seed-memory: Store previous incident knowledge through the
 *    same Hindsight retain flow used by the real postmortem API.
 * 3. POST /api/demo/similar-incident: Trigger a similar incident to demonstrate
 *    that historical memory dynamically influences the AI recommendation.
 * 
 * NOTE: The final AI diagnosis is NOT hardcoded; it is dynamically generated
 * from the recalled memory.
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final HindsightClient hindsightClient;
    private final PostmortemService postmortemService;
    private final IncidentService incidentService;

    /**
     * POST /api/demo/reset
     * Clears demo memory store so presenters can start fresh.
     * (HACKATHON PROTOTYPE ONLY)
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        log.info("[HACKATHON DEMO] Resetting memory store for clean demonstration.");
        hindsightClient.clearMemory();
        return ResponseEntity.ok(Map.of(
                "status", "RESET_SUCCESSFUL",
                "message", "Demo memory reset successfully. IncidentPilot is now in a clean zero-memory state."
        ));
    }

    /**
     * POST /api/demo/seed-memory
     * Stores historical incident knowledge through the same Hindsight retain flow
     * used by the real postmortem API.
     * (HACKATHON PROTOTYPE ONLY)
     */
    @PostMapping("/seed-memory")
    public ResponseEntity<PostmortemResponse> seedMemory() {
        log.info("[HACKATHON DEMO] Seeding memory through the real postmortem retain flow.");

        // Exact seed data specified for hackathon demonstration:
        // Previous Incident:
        // Service: Checkout API
        // Symptoms: High latency and database connection timeouts
        // Root Cause: Database connection leak
        // Successful Fix: Corrected connection lifecycle handling
        // Failed Approach: Increasing connection pool size
        // Prevention: Connection monitoring
        PostmortemRequest seedRequest = PostmortemRequest.builder()
                .incidentId("INC-DEMO-001")
                .serviceName("Checkout API")
                .symptoms("High latency and database connection timeouts")
                .actualRootCause("Database connection leak")
                .successfulFix("Corrected connection lifecycle handling")
                .failedApproaches("Increasing database connection pool size")
                .preventionStrategy("Connection monitoring")
                .lessonsLearned("Check connection lifecycle before increasing pool capacity")
                .build();

        // Stored through the same Hindsight retain flow used by the real postmortem API
        PostmortemResponse response = postmortemService.processPostmortem(seedRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/demo/similar-incident
     * Analyzes a similar incident to demonstrate how the seeded memory dynamically
     * guides the AI recommendations without hardcoding.
     * (HACKATHON PROTOTYPE ONLY)
     */
    @PostMapping("/similar-incident")
    public ResponseEntity<IncidentAnalysisResponse> similarIncident(
            @RequestBody(required = false) IncidentAnalysisRequest customRequest) {

        log.info("[HACKATHON DEMO] Triggering similar incident analysis to showcase memory-guided reasoning.");

        IncidentAnalysisRequest request;
        if (customRequest != null && customRequest.getServiceName() != null) {
            request = customRequest;
        } else {
            // Default demo incident targeting Checkout API with connection symptoms
            request = IncidentAnalysisRequest.builder()
                    .serviceName("Checkout API")
                    .symptoms("High latency and database connection timeouts")
                    .logs("ERROR: Connection pool exhausted - timeout waiting for connection from HikariPool")
                    .severity("SEV-1")
                    .build();
        }

        // Executes full 6-step analysis flow (Recall -> Prompt -> LLM -> Diagnosis)
        IncidentAnalysisResponse response = incidentService.analyze(request);
        return ResponseEntity.ok(response);
    }
}