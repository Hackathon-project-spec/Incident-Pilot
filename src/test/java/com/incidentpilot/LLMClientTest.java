package com.incidentpilot;

import com.incidentpilot.client.LLMClient;
import com.incidentpilot.dto.IncidentAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LLMClientTest {

    @Autowired
    private LLMClient llmClient;

    @Test
    void testWhenHindsightReturnsEmptyList_NoMemoryFound() {
        // Given empty memory []
        IncidentAnalysisResponse response = llmClient.analyzeIncident(
                "Checkout API",
                "Latency increased from 200ms to 4 seconds",
                "Database connection timeout errors",
                "SEV-1",
                Collections.emptyList()
        );

        assertNotNull(response);
        assertEquals("Checkout API", response.getServiceName());
        assertEquals("SEV-1", response.getSeverity());

        // Must NOT fabricate historical incidents
        assertTrue(response.getHistoricalIncidents().contains("No relevant historical incidents found."));

        // memoryBasedInsights should clearly indicate no historical memory was found
        boolean insightReflectsNoMemory = response.getMemoryBasedInsights().stream()
                .anyMatch(insight -> insight.toLowerCase().contains("no relevant historical memory")
                        || insight.toLowerCase().contains("no relevant historical"));
        assertTrue(insightReflectsNoMemory, "Should clearly state no historical memory was found");
    }

    @Test
    void testWhenHindsightReturnsPreviousIncident_MemoryInfluencesRecommendation() {
        // Given: The exact example from user specification
        String historicalMemory = "Previous Checkout API incident was caused by a database connection leak. " +
                "Previous successful fix: Corrected connection lifecycle. " +
                "Previous failed approach: Increasing connection pool size.";

        // When
        IncidentAnalysisResponse response = llmClient.analyzeIncident(
                "Checkout API",
                "Latency increased from 200ms to 4 seconds",
                "Database connection timeout errors",
                "SEV-1",
                List.of(historicalMemory)
        );

        assertNotNull(response);
        assertTrue(response.getHistoricalIncidents().contains(historicalMemory));

        // The recommendation should recommend checking connection lifecycle before increasing connection pool size
        boolean foundRecommendedFix = response.getRecommendedFixes().stream()
                .anyMatch(fix -> fix.toLowerCase().contains("connection lifecycle")
                        && fix.toLowerCase().contains("increasing connection pool size"));
        assertTrue(foundRecommendedFix, "Recommendation must dynamically integrate successful fix and warn against failed approach: " + response.getRecommendedFixes());

        // Memory-based insights should make it obvious historical memory influenced the recommendation
        boolean memoryInfluenceObvious = response.getMemoryBasedInsights().stream()
                .anyMatch(insight -> insight.toLowerCase().contains("historical memory directly influenced")
                        || insight.toLowerCase().contains("proven fix")
                        || insight.toLowerCase().contains("corrected connection lifecycle"));
        assertTrue(memoryInfluenceObvious, "Memory-based insight should make it obvious historical memory influenced the recommendation: " + response.getMemoryBasedInsights());
    }

    @Test
    void testDynamicMemoryGenerationWithDifferentServiceAndMemory() {
        // Proves that response is dynamically generated from actual recalled memory, NOT hardcoded
        String differentMemory = "Previous Inventory Service incident was caused by thread pool exhaustion. " +
                "Previous successful fix: Tuned worker thread pool and queue depth. " +
                "Previous failed approach: Doubling container memory.";

        IncidentAnalysisResponse response = llmClient.analyzeIncident(
                "Inventory Service",
                "Orders stuck in processing",
                "RejectedExecutionException: Thread pool full",
                "SEV-2",
                List.of(differentMemory)
        );

        assertNotNull(response);
        assertEquals("Inventory Service", response.getServiceName());

        // Check that it dynamically picked up the new fix and new failed approach
        boolean dynamicallyDerived = response.getRecommendedFixes().stream()
                .anyMatch(fix -> fix.toLowerCase().contains("tuned worker thread pool")
                        && fix.toLowerCase().contains("doubling container memory"));
        assertTrue(dynamicallyDerived, "Must dynamically derive from new memory: " + response.getRecommendedFixes());
    }

    @Test
    void testSafetyConstraints_NoCommandsOrRemediationClaims() {
        IncidentAnalysisResponse response = llmClient.analyzeIncident(
                "Order Service",
                "High latency",
                "CPU 95%",
                "SEV-2",
                List.of()
        );

        for (String fix : response.getRecommendedFixes()) {
            assertFalse(fix.toLowerCase().startsWith("i fixed"), "Fix should not claim executed remediation: " + fix);
            assertFalse(fix.toLowerCase().startsWith("i executed"), "Fix should not claim command execution: " + fix);
        }
    }
}