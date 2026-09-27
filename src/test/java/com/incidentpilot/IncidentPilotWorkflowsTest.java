package com.incidentpilot;

import com.incidentpilot.controller.IncidentController;
import com.incidentpilot.controller.PostmortemController;
import com.incidentpilot.dto.IncidentAnalysisRequest;
import com.incidentpilot.dto.IncidentAnalysisResponse;
import com.incidentpilot.dto.IncidentRequest;
import com.incidentpilot.dto.IncidentResponse;
import com.incidentpilot.dto.PostmortemRequest;
import com.incidentpilot.dto.PostmortemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IncidentPilotWorkflowsTest {

    @Autowired
    private IncidentController incidentController;

    @Autowired
    private PostmortemController postmortemController;

    @Test
    void testMinimalPostmortemWorkflow_RetainAndReflect() {
        // Given: The exact request specified by the user
        PostmortemRequest request = PostmortemRequest.builder()
                .incidentId("INC-001")
                .serviceName("Checkout API")
                .actualRootCause("Database connection leak")
                .successfulFix("Corrected connection lifecycle handling")
                .failedApproaches("Increasing database connection pool size")
                .preventionStrategy("Added connection monitoring")
                .lessonsLearned("Check connection lifecycle before increasing pool capacity")
                .build();

        // When: PostmortemController -> PostmortemService -> Hindsight retain -> Hindsight reflect
        ResponseEntity<PostmortemResponse> responseEntity = postmortemController.submitPostmortem(request);

        // Then: Validate honest state reporting
        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());
        PostmortemResponse response = responseEntity.getBody();
        assertNotNull(response);

        assertEquals("INC-001", response.getIncidentId());
        assertEquals("Checkout API", response.getServiceName());
        assertEquals("SUCCESS", response.getRetentionStatus());
        assertNotNull(response.getRetentionId());
        assertEquals("SUCCESS", response.getReflectionStatus());
        assertNotNull(response.getReflectionSummary());
        assertEquals("COMPLETED", response.getStatus());
        assertTrue(response.getMessage().contains("retained into long-term memory"));

        // Demonstration: The resolved incident has become long-term memory!
        // When we now analyze a new incident on "Checkout API", the retained memory is recalled
        IncidentAnalysisRequest newIncident = IncidentAnalysisRequest.builder()
                .serviceName("Checkout API")
                .symptoms("Latency increased to 4s")
                .logs("Database connection timeout errors")
                .severity("SEV-1")
                .build();

        ResponseEntity<IncidentAnalysisResponse> analysisResponse = incidentController.analyze(newIncident);
        assertNotNull(analysisResponse);
        IncidentAnalysisResponse diagnosis = analysisResponse.getBody();
        assertNotNull(diagnosis);

        // It recalled INC-001 from long-term memory
        boolean recalledRetainedMemory = diagnosis.getHistoricalIncidents().stream()
                .anyMatch(hist -> hist.contains("INC-001") || hist.contains("Database connection leak"));
        assertTrue(recalledRetainedMemory, "Postmortem must be recalled from long-term memory: " + diagnosis.getHistoricalIncidents());
    }

    @Test
    void testIncidentAnalyzeAPI() {
        IncidentAnalysisRequest request = IncidentAnalysisRequest.builder()
                .serviceName("Payments API")
                .symptoms("Latency increased from 200ms to 4 seconds")
                .logs("Connection timeout errors")
                .severity("SEV-1")
                .build();

        ResponseEntity<IncidentAnalysisResponse> responseEntity = incidentController.analyze(request);

        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());
        IncidentAnalysisResponse response = responseEntity.getBody();
        assertNotNull(response);

        assertEquals("Payments API", response.getServiceName());
        assertEquals("SEV-1", response.getSeverity());
        assertNotNull(response.getPossibleRootCauses());
        assertNotNull(response.getInvestigationSteps());
        assertNotNull(response.getRecommendedFixes());
        assertNotNull(response.getHistoricalIncidents());
        assertNotNull(response.getMemoryBasedInsights());
    }

    @Test
    void testWorkflow1_LegacyDiagnose() {
        IncidentRequest request = IncidentRequest.builder()
                .incidentId("INC-1001")
                .title("Database connection pool exhaustion")
                .description("API gateway returning 504 Gateway Timeout, connection pool at 100%")
                .service("order-service")
                .severity("CRITICAL")
                .logs("ERROR: HikariPool-1 - Connection is not available, request timed out after 30000ms.")
                .metrics(Map.of("active_connections", 100, "waiting_threads", 45))
                .timestamp("2026-09-28T00:50:00Z")
                .build();

        ResponseEntity<IncidentResponse> responseEntity = incidentController.diagnose(request);

        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());
        IncidentResponse response = responseEntity.getBody();
        assertNotNull(response);
        assertEquals("INC-1001", response.getIncidentId());
        assertEquals("DIAGNOSED", response.getStatus());
    }
}