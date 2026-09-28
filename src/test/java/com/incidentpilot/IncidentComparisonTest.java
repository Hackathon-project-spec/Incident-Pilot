package com.incidentpilot;

import com.incidentpilot.controller.DemoController;
import com.incidentpilot.dto.IncidentAnalysisRequest;
import com.incidentpilot.dto.IncidentAnalysisResponse;
import com.incidentpilot.dto.IncidentComparisonResponse;
import com.incidentpilot.service.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IncidentComparisonTest {

    @Autowired
    private DemoController demoController;

    @Autowired
    private IncidentService incidentService;

    @BeforeEach
    void setup() {
        demoController.reset();
    }

    @Test
    void testComparison_WithoutMemoryVsWithMemory_CoreDifferentiator() {
        // 1. Seed the memory with Checkout API postmortem
        demoController.seedMemory();

        // 2. Run the side-by-side comparison
        IncidentAnalysisRequest request = IncidentAnalysisRequest.builder()
                .serviceName("Checkout API")
                .symptoms("High latency and database connection timeouts")
                .logs("ERROR: Connection pool exhausted - timeout waiting for connection from HikariPool")
                .severity("SEV-1")
                .build();

        ResponseEntity<IncidentComparisonResponse> comparisonResponse = demoController.compare(request);
        assertNotNull(comparisonResponse);
        assertEquals(200, comparisonResponse.getStatusCode().value());

        IncidentComparisonResponse comparison = comparisonResponse.getBody();
        assertNotNull(comparison);
        assertTrue(comparison.isMemoryAdvantageDemonstrated());

        // Baseline Diagnosis (WITHOUT MEMORY)
        IncidentAnalysisResponse withoutMem = comparison.getWithoutMemoryDiagnosis();
        assertNotNull(withoutMem);
        assertTrue(withoutMem.getHistoricalIncidents().contains("No relevant historical incidents found."));
        assertTrue(withoutMem.getMemoryBasedInsights().stream()
                .anyMatch(i -> i.toLowerCase().contains("no relevant historical memory")));

        // Augmented Diagnosis (WITH MEMORY)
        IncidentAnalysisResponse withMem = comparison.getWithMemoryDiagnosis();
        assertNotNull(withMem);
        assertTrue(withMem.getHistoricalIncidents().stream()
                .anyMatch(h -> h.contains("INC-DEMO-001") || h.contains("connection leak")));

        // Verify withMem prioritized proven fix and warned against failed pool size increase
        boolean recommendedFixHasProvenAction = withMem.getRecommendedFixes().stream()
                .anyMatch(f -> f.toLowerCase().contains("connection lifecycle")
                        && f.toLowerCase().contains("connection pool size"));
        assertTrue(recommendedFixHasProvenAction, "Memory-augmented diagnosis must prioritize proven fix and caution against failed pool size increase");

        // Verify failedApproachesToAvoid is populated
        assertFalse(withMem.getFailedApproachesToAvoid().isEmpty());
        assertTrue(withMem.getFailedApproachesToAvoid().stream()
                .anyMatch(a -> a.toLowerCase().contains("connection pool size")));

        // Verify comparison highlights
        assertFalse(comparison.getComparisonHighlights().isEmpty());
        assertTrue(comparison.getComparisonHighlights().stream()
                .anyMatch(h -> h.contains("Root Cause Precision") || h.contains("Prevented")));
    }

    @Test
    void testMultiScenarioSeedAllAndComparison() {
        // Seed all 4 realistic incident postmortems
        ResponseEntity<Map<String, Object>> seedAllResp = demoController.seedAll();
        assertNotNull(seedAllResp);
        assertEquals(200, seedAllResp.getStatusCode().value());
        assertEquals("ALL_SEEDS_STORED", seedAllResp.getBody().get("status"));

        // Test Scenario 2: Auth Service JWKS Cache Invalidation
        IncidentAnalysisRequest authRequest = IncidentAnalysisRequest.builder()
                .serviceName("Auth Service")
                .symptoms("Intermittent 401 Unauthorized errors and token validation failures")
                .logs("JWT verification failed: Key ID (kid) not found in local keystore cache")
                .severity("SEV-1")
                .build();

        IncidentComparisonResponse authComparison = incidentService.compareWithAndWithoutMemory(authRequest);
        assertNotNull(authComparison);
        assertTrue(authComparison.isMemoryAdvantageDemonstrated());
        assertTrue(authComparison.getWithMemoryDiagnosis().getRecommendedFixes().stream()
                .anyMatch(f -> f.toLowerCase().contains("jwks") || f.toLowerCase().contains("cache")));

        // Test Scenario 3: Order Processing Service Blocking Thread Pool
        IncidentAnalysisRequest orderRequest = IncidentAnalysisRequest.builder()
                .serviceName("Order Processing Service")
                .symptoms("Orders stuck in PENDING status, high CPU, RejectedExecutionException")
                .logs("java.util.concurrent.RejectedExecutionException: Task rejected from ThreadPoolExecutor")
                .severity("SEV-2")
                .build();

        IncidentComparisonResponse orderComparison = incidentService.compareWithAndWithoutMemory(orderRequest);
        assertNotNull(orderComparison);
        assertTrue(orderComparison.isMemoryAdvantageDemonstrated());
        assertTrue(orderComparison.getWithMemoryDiagnosis().getRecommendedFixes().stream()
                .anyMatch(f -> f.toLowerCase().contains("webclient") || f.toLowerCase().contains("thread pool") || f.toLowerCase().contains("non-blocking")));

        // Test Scenario 4: Notification Service Kafka Poison Pill
        IncidentAnalysisRequest notifRequest = IncidentAnalysisRequest.builder()
                .serviceName("Notification Service")
                .symptoms("Push notifications delayed by 30+ minutes, growing Kafka consumer lag")
                .logs("org.apache.kafka.common.errors.SerializationException: Error deserializing JSON message")
                .severity("SEV-2")
                .build();

        IncidentComparisonResponse notifComparison = incidentService.compareWithAndWithoutMemory(notifRequest);
        assertNotNull(notifComparison);
        assertTrue(notifComparison.isMemoryAdvantageDemonstrated());
        assertTrue(notifComparison.getWithMemoryDiagnosis().getRecommendedFixes().stream()
                .anyMatch(f -> f.toLowerCase().contains("dead letter queue") || f.toLowerCase().contains("dlq")));
    }

    @Test
    void testStructuredOutputFormat_AllKeysPresent() {
        demoController.seedMemory();

        IncidentAnalysisResponse diagnosis = incidentService.analyze(null);

        // Structured output verification:
        // 1. root cause hypothesis
        assertNotNull(diagnosis.getPossibleRootCauses());
        assertFalse(diagnosis.getPossibleRootCauses().isEmpty());
        assertNotNull(diagnosis.getRootCauseHypothesis());

        // 2. investigation steps
        assertNotNull(diagnosis.getInvestigationSteps());
        assertFalse(diagnosis.getInvestigationSteps().isEmpty());

        // 3. possible / recommended fixes
        assertNotNull(diagnosis.getRecommendedFixes());
        assertFalse(diagnosis.getRecommendedFixes().isEmpty());
        assertNotNull(diagnosis.getPossibleFixes());

        // 4. related past incidents
        assertNotNull(diagnosis.getHistoricalIncidents());
        assertFalse(diagnosis.getHistoricalIncidents().isEmpty());
        assertNotNull(diagnosis.getRelatedPastIncidents());

        // 5. memory-based insights
        assertNotNull(diagnosis.getMemoryBasedInsights());
        assertFalse(diagnosis.getMemoryBasedInsights().isEmpty());

        // 6. failed approaches to avoid
        assertNotNull(diagnosis.getFailedApproachesToAvoid());
        assertFalse(diagnosis.getFailedApproachesToAvoid().isEmpty());

        // 7. confidence score
        assertNotNull(diagnosis.getConfidenceScore());
        assertTrue(diagnosis.getConfidenceScore().contains("HIGH"));
    }
}
