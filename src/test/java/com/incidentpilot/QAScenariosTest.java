package com.incidentpilot;

import com.incidentpilot.client.HindsightClient;
import com.incidentpilot.controller.DemoController;
import com.incidentpilot.controller.IncidentController;
import com.incidentpilot.controller.PostmortemController;
import com.incidentpilot.dto.IncidentAnalysisRequest;
import com.incidentpilot.dto.IncidentAnalysisResponse;
import com.incidentpilot.dto.PostmortemRequest;
import com.incidentpilot.dto.PostmortemResponse;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QA Verification Test Suite for Hackathon Demonstration.
 * 
 * Verifies the 4 critical demo scenarios in exact sequence:
 * - TEST 1: New incident with no historical memory.
 * - TEST 2: Seed Checkout API incident, then analyze similar incident.
 * - TEST 3: Submit a postmortem (retain + reflect).
 * - TEST 4: Submit similar incident to newly retained postmortem and verify influence.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QAScenariosTest {

    @Autowired
    private IncidentController incidentController;

    @Autowired
    private PostmortemController postmortemController;

    @Autowired
    private DemoController demoController;

    @Autowired
    private HindsightClient hindsightClient;

    @Test
    @Order(1)
    void test1_NewIncidentWithNoHistoricalMemory() {
        // Ensure clean memory baseline
        hindsightClient.clearMemory();

        IncidentAnalysisRequest request = IncidentAnalysisRequest.builder()
                .serviceName("Billing API")
                .symptoms("HTTP 500 error rate spiked to 15%")
                .logs("NullPointerException in InvoiceGenerator.java:142")
                .severity("SEV-2")
                .build();

        ResponseEntity<IncidentAnalysisResponse> responseEntity = incidentController.analyze(request);

        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());
        IncidentAnalysisResponse diagnosis = responseEntity.getBody();
        assertNotNull(diagnosis);

        assertEquals("Billing API", diagnosis.getServiceName());
        assertEquals("SEV-2", diagnosis.getSeverity());

        // Expected: Generic investigation recommendations
        assertNotNull(diagnosis.getInvestigationSteps());
        assertFalse(diagnosis.getInvestigationSteps().isEmpty(), "Investigation steps must be provided");
        assertNotNull(diagnosis.getPossibleRootCauses());
        assertFalse(diagnosis.getPossibleRootCauses().isEmpty(), "Possible root causes must be provided");
        assertNotNull(diagnosis.getRecommendedFixes());
        assertFalse(diagnosis.getRecommendedFixes().isEmpty(), "Recommended fixes must be provided");

        // Expected: Clearly state that no relevant historical memory was found (no fabrication)
        assertTrue(diagnosis.getHistoricalIncidents().contains("No relevant historical incidents found."),
                "historicalIncidents must state that no memory was found: " + diagnosis.getHistoricalIncidents());

        boolean statesNoMemory = diagnosis.getMemoryBasedInsights().stream()
                .anyMatch(insight -> insight.toLowerCase().contains("no relevant historical memory")
                        || insight.toLowerCase().contains("no relevant historical"));
        assertTrue(statesNoMemory, "Insights must state no historical memory was found: " + diagnosis.getMemoryBasedInsights());
    }

    @Test
    @Order(2)
    void test2_SeedCheckoutApiMemory_AndAnalyzeSimilarIncident() {
        // Step A: Seed memory using demo seed flow
        ResponseEntity<PostmortemResponse> seedResponse = demoController.seedMemory();
        assertNotNull(seedResponse);
        assertEquals(200, seedResponse.getStatusCode().value());
        PostmortemResponse seedBody = seedResponse.getBody();
        assertNotNull(seedBody);
        assertEquals("SUCCESS", seedBody.getRetentionStatus());

        // Step B: Submit similar Checkout API incident
        IncidentAnalysisRequest request = IncidentAnalysisRequest.builder()
                .serviceName("Checkout API")
                .symptoms("Latency increased from 200ms to 4 seconds, database connection timeouts")
                .logs("ERROR: Connection pool exhausted - timeout waiting for connection from HikariPool")
                .severity("SEV-1")
                .build();

        ResponseEntity<IncidentAnalysisResponse> responseEntity = incidentController.analyze(request);
        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());
        IncidentAnalysisResponse diagnosis = responseEntity.getBody();
        assertNotNull(diagnosis);

        // Expected: Hindsight recalls the previous incident
        assertFalse(diagnosis.getHistoricalIncidents().isEmpty());
        boolean recalledIncident = diagnosis.getHistoricalIncidents().stream()
                .anyMatch(hist -> hist.contains("Checkout API") && hist.contains("Database connection leak"));
        assertTrue(recalledIncident, "Previous incident must be recalled: " + diagnosis.getHistoricalIncidents());

        // Expected: Response references previous incident and actual successful/failed approaches
        boolean referencesFixAndFailed = diagnosis.getRecommendedFixes().stream()
                .anyMatch(fix -> fix.toLowerCase().contains("connection lifecycle")
                        && (fix.toLowerCase().contains("connection pool size") || fix.toLowerCase().contains("increasing database connection pool size")));
        assertTrue(referencesFixAndFailed, "Response must reference successful fix and failed approach: " + diagnosis.getRecommendedFixes());

        boolean insightsExplainInfluence = diagnosis.getMemoryBasedInsights().stream()
                .anyMatch(insight -> insight.toLowerCase().contains("historical memory directly influenced")
                        || insight.toLowerCase().contains("prioritizing proven fix")
                        || insight.toLowerCase().contains("connection lifecycle"));
        assertTrue(insightsExplainInfluence, "Insights must explain the memory influence: " + diagnosis.getMemoryBasedInsights());
    }

    @Test
    @Order(3)
    void test3_SubmitPostmortem_RetainAndReflect() {
        // Given: Postmortem for a different service (Auth Service)
        PostmortemRequest request = PostmortemRequest.builder()
                .incidentId("INC-AUTH-099")
                .serviceName("Auth Service")
                .symptoms("All login attempts timing out with SSL handshake errors")
                .actualRootCause("Expired TLS certificate on LDAP identity provider")
                .successfulFix("Rotated identity provider TLS certificate and enabled auto-renewal")
                .failedApproaches("Restarting auth pod containers repeatedly")
                .preventionStrategy("Certificate expiration alerts at 30, 15, and 7 days")
                .lessonsLearned("Always monitor external identity provider certificate lifecycles")
                .build();

        // When: Submit postmortem
        ResponseEntity<PostmortemResponse> responseEntity = postmortemController.submitPostmortem(request);

        // Then: Postmortem is retained and reflection is triggered
        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());
        PostmortemResponse response = responseEntity.getBody();
        assertNotNull(response);

        // Expected: Retained in Hindsight
        assertEquals("INC-AUTH-099", response.getIncidentId());
        assertEquals("Auth Service", response.getServiceName());
        assertEquals("SUCCESS", response.getRetentionStatus());
        assertNotNull(response.getRetentionId());

        // Expected: Reflection triggered and consolidated
        assertEquals("SUCCESS", response.getReflectionStatus());
        assertNotNull(response.getReflectionSummary());
        assertEquals("COMPLETED", response.getStatus());
        assertTrue(response.getMessage().contains("retained into long-term memory"));
    }

    @Test
    @Order(4)
    void test4_SubmitSimilarIncident_NewlyRetainedKnowledgeInfluencesResponse() {
        // When: Submit similar incident for Auth Service
        IncidentAnalysisRequest request = IncidentAnalysisRequest.builder()
                .serviceName("Auth Service")
                .symptoms("Authentication timeouts and handshake failures")
                .logs("SSLHandshakeException: PKIX path validation failed: unable to find valid certification path")
                .severity("SEV-1")
                .build();

        ResponseEntity<IncidentAnalysisResponse> responseEntity = incidentController.analyze(request);
        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());
        IncidentAnalysisResponse diagnosis = responseEntity.getBody();
        assertNotNull(diagnosis);

        // Expected: The newly retained knowledge from INC-AUTH-099 influences the response
        boolean recalledAuthMemory = diagnosis.getHistoricalIncidents().stream()
                .anyMatch(hist -> hist.contains("INC-AUTH-099") || hist.contains("Expired TLS certificate"));
        assertTrue(recalledAuthMemory, "Newly retained memory must be recalled: " + diagnosis.getHistoricalIncidents());

        // Expected: Recommends the newly proven fix and warns against the failed approach
        boolean referencesAuthFixAndFailed = diagnosis.getRecommendedFixes().stream()
                .anyMatch(fix -> fix.toLowerCase().contains("rotated identity provider tls certificate")
                        || fix.toLowerCase().contains("tls certificate")
                        || fix.toLowerCase().contains("restarting auth pod containers"));
        assertTrue(referencesAuthFixAndFailed, "Response must reflect newly retained learnings: " + diagnosis.getRecommendedFixes());

        // Expected: Memory insights reflect the newly retained knowledge
        boolean insightReflectsAuth = diagnosis.getMemoryBasedInsights().stream()
                .anyMatch(insight -> insight.toLowerCase().contains("proven fix")
                        || insight.toLowerCase().contains("tls certificate")
                        || insight.toLowerCase().contains("historical memory directly influenced"));
        assertTrue(insightReflectsAuth, "Insights must reflect new learnings: " + diagnosis.getMemoryBasedInsights());
    }
}