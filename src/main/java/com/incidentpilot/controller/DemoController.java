package com.incidentpilot.controller;

import com.incidentpilot.client.HindsightClient;
import com.incidentpilot.dto.IncidentAnalysisRequest;
import com.incidentpilot.dto.IncidentAnalysisResponse;
import com.incidentpilot.dto.IncidentComparisonResponse;
import com.incidentpilot.dto.PostmortemRequest;
import com.incidentpilot.dto.PostmortemResponse;
import com.incidentpilot.service.IncidentService;
import com.incidentpilot.service.PostmortemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
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
 * 3. POST /api/demo/seed-all: Seed multiple diverse postmortems across services.
 * 4. POST /api/demo/compare: Run the "Without Memory" vs "With Memory" comparison
 *    showcasing the direct advantage of memory augmentation.
 * 5. POST /api/demo/similar-incident: Trigger similar incident analysis.
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DemoController {

    private final HindsightClient hindsightClient;
    private final PostmortemService postmortemService;
    private final IncidentService incidentService;

    /**
     * POST /api/demo/reset
     * Clears demo memory store so presenters can start fresh.
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
     * Stores canonical Checkout API incident knowledge through the Hindsight retain flow.
     */
    @PostMapping("/seed-memory")
    public ResponseEntity<PostmortemResponse> seedMemory() {
        log.info("[HACKATHON DEMO] Seeding Checkout API memory through the postmortem retain flow.");

        PostmortemRequest seedRequest = PostmortemRequest.builder()
                .incidentId("INC-DEMO-001")
                .serviceName("Checkout API")
                .symptoms("High latency and database connection timeouts")
                .actualRootCause("Database connection leak in payment processing loop")
                .successfulFix("Corrected connection lifecycle handling using try-with-resources")
                .failedApproaches("Increasing database connection pool size (HikariCP maxPoolSize)")
                .preventionStrategy("Connection leak detection threshold monitoring and static analysis")
                .lessonsLearned("Check connection lifecycle before increasing pool capacity")
                .build();

        PostmortemResponse response = postmortemService.processPostmortem(seedRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/demo/seed-all
     * Preloads 4 realistic incident postmortems into Hindsight for multi-scenario demonstrations.
     */
    @PostMapping("/seed-all")
    public ResponseEntity<Map<String, Object>> seedAll() {
        log.info("[HACKATHON DEMO] Seeding all 4 realistic incident postmortems.");

        List<PostmortemRequest> seeds = List.of(
                // Scenario 1: Checkout API
                PostmortemRequest.builder()
                        .incidentId("INC-DEMO-001")
                        .serviceName("Checkout API")
                        .symptoms("High latency and database connection timeouts")
                        .actualRootCause("Database connection leak in payment processing loop")
                        .successfulFix("Corrected connection lifecycle handling using try-with-resources")
                        .failedApproaches("Increasing database connection pool size (HikariCP maxPoolSize)")
                        .preventionStrategy("Connection leak detection threshold monitoring and static analysis")
                        .lessonsLearned("Check connection lifecycle before increasing pool capacity")
                        .build(),

                // Scenario 2: Auth Service
                PostmortemRequest.builder()
                        .incidentId("INC-AUTH-042")
                        .serviceName("Auth Service")
                        .symptoms("Intermittent 401 Unauthorized errors and token validation failures")
                        .actualRootCause("JWKS public key rotation cache mismatch")
                        .successfulFix("Evicted stale JWKS cache and added dynamic cache-eviction hook on rotation")
                        .failedApproaches("Restarting auth pods without clearing distributed cache")
                        .preventionStrategy("Automated JWKS key refresh on unrecognized key ID (kid)")
                        .lessonsLearned("Always invalidate local keystore cache upon public key rotation")
                        .build(),

                // Scenario 3: Order Processing Service
                PostmortemRequest.builder()
                        .incidentId("INC-ORD-108")
                        .serviceName("Order Processing Service")
                        .symptoms("Orders stuck in PENDING status, high CPU, RejectedExecutionException")
                        .actualRootCause("Worker thread pool exhaustion caused by blocking synchronous downstream I/O")
                        .successfulFix("Converted downstream call to non-blocking WebClient with circuit breaker")
                        .failedApproaches("Doubling worker thread pool size (caused OutOfMemory and context switching storm)")
                        .preventionStrategy("Enforce non-blocking asynchronous I/O across worker executors")
                        .lessonsLearned("Do not perform blocking HTTP calls inside fixed thread pool executors")
                        .build(),

                // Scenario 4: Notification Service
                PostmortemRequest.builder()
                        .incidentId("INC-NOTIF-552")
                        .serviceName("Notification Service")
                        .symptoms("Push notifications delayed by 30+ minutes, growing Kafka consumer lag")
                        .actualRootCause("Poison pill deserialization error causing infinite consumer retry loop")
                        .successfulFix("Configured Dead Letter Queue (DLQ) error handler to skip and log malformed payloads")
                        .failedApproaches("Increasing partition count and adding more consumer replicas")
                        .preventionStrategy("Strict producer payload schema validation at API gateway")
                        .lessonsLearned("Always route unparseable messages to DLQ to prevent consumer head-of-line blocking")
                        .build()
        );

        List<PostmortemResponse> responses = new ArrayList<>();
        for (PostmortemRequest seed : seeds) {
            responses.add(postmortemService.processPostmortem(seed));
        }

        return ResponseEntity.ok(Map.of(
                "status", "ALL_SEEDS_STORED",
                "seededCount", responses.size(),
                "details", responses
        ));
    }

    /**
     * POST /api/demo/compare
     * Side-by-side comparison of "Without Memory" vs "With Memory" on an incident.
     */
    @PostMapping("/compare")
    public ResponseEntity<IncidentComparisonResponse> compare(
            @RequestBody(required = false) IncidentAnalysisRequest customRequest) {
        log.info("[HACKATHON DEMO] Executing 'Without Memory' vs 'With Memory' comparison.");
        return ResponseEntity.ok(incidentService.compareWithAndWithoutMemory(customRequest));
    }

    /**
     * GET /api/demo/compare
     * 1-click browser test for the comparison differentiator.
     */
    @GetMapping("/compare")
    public ResponseEntity<IncidentComparisonResponse> getCompare() {
        return ResponseEntity.ok(incidentService.compareWithAndWithoutMemory(null));
    }

    /**
     * POST /api/demo/similar-incident
     * Analyzes a similar incident to demonstrate how seeded memory guides the diagnosis.
     */
    @PostMapping("/similar-incident")
    public ResponseEntity<IncidentAnalysisResponse> similarIncident(
            @RequestBody(required = false) IncidentAnalysisRequest customRequest) {

        log.info("[HACKATHON DEMO] Triggering similar incident analysis to showcase memory-guided reasoning.");

        IncidentAnalysisRequest request;
        if (customRequest != null && customRequest.getServiceName() != null) {
            request = customRequest;
        } else {
            request = IncidentAnalysisRequest.builder()
                    .serviceName("Checkout API")
                    .symptoms("High latency and database connection timeouts")
                    .logs("ERROR: Connection pool exhausted - timeout waiting for connection from HikariPool")
                    .severity("SEV-1")
                    .build();
        }

        IncidentAnalysisResponse response = incidentService.analyze(request);
        return ResponseEntity.ok(response);
    }
}