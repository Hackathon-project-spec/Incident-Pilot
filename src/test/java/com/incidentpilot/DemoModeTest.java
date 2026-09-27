package com.incidentpilot;

import com.incidentpilot.controller.DemoController;
import com.incidentpilot.dto.IncidentAnalysisResponse;
import com.incidentpilot.dto.PostmortemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DemoModeTest {

    @Autowired
    private DemoController demoController;

    @Test
    void testCompleteDemoModeWorkflow() {
        // STEP 1: Reset demo memory to clean state
        ResponseEntity<Map<String, String>> resetResponse = demoController.reset();
        assertNotNull(resetResponse);
        assertEquals(200, resetResponse.getStatusCode().value());
        assertEquals("RESET_SUCCESSFUL", resetResponse.getBody().get("status"));

        // STEP 2: Seed memory using the same Hindsight retain flow
        ResponseEntity<PostmortemResponse> seedResponse = demoController.seedMemory();
        assertNotNull(seedResponse);
        assertEquals(200, seedResponse.getStatusCode().value());
        PostmortemResponse seedBody = seedResponse.getBody();
        assertNotNull(seedBody);
        assertEquals("INC-DEMO-001", seedBody.getIncidentId());
        assertEquals("Checkout API", seedBody.getServiceName());
        assertEquals("SUCCESS", seedBody.getRetentionStatus());

        // STEP 3: Trigger similar incident to demonstrate dynamic memory recall and influence
        ResponseEntity<IncidentAnalysisResponse> similarIncidentResponse = demoController.similarIncident(null);
        assertNotNull(similarIncidentResponse);
        assertEquals(200, similarIncidentResponse.getStatusCode().value());
        IncidentAnalysisResponse diagnosis = similarIncidentResponse.getBody();
        assertNotNull(diagnosis);

        assertEquals("Checkout API", diagnosis.getServiceName());

        // Verify the seeded memory was recalled
        boolean recalledSeededMemory = diagnosis.getHistoricalIncidents().stream()
                .anyMatch(hist -> hist.contains("INC-DEMO-001") || hist.contains("Database connection leak"));
        assertTrue(recalledSeededMemory, "Seeded memory must be recalled: " + diagnosis.getHistoricalIncidents());

        // Verify that recommendations dynamically reflect the successful fix and failed approach
        boolean recommendsLifecycleBeforePoolSize = diagnosis.getRecommendedFixes().stream()
                .anyMatch(fix -> fix.toLowerCase().contains("connection lifecycle")
                        && (fix.toLowerCase().contains("connection pool size") || fix.toLowerCase().contains("database connection pool size")));
        assertTrue(recommendsLifecycleBeforePoolSize,
                "Recommendation must dynamically reflect the seeded memory learnings: " + diagnosis.getRecommendedFixes());

        // Verify memory-based insights make the influence obvious
        boolean insightExplainsInfluence = diagnosis.getMemoryBasedInsights().stream()
                .anyMatch(insight -> insight.toLowerCase().contains("historical memory directly influenced")
                        || insight.toLowerCase().contains("proven fix")
                        || insight.toLowerCase().contains("connection lifecycle"));
        assertTrue(insightExplainsInfluence,
                "Memory-based insights must explain the historical influence: " + diagnosis.getMemoryBasedInsights());
    }
}