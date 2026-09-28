package com.incidentpilot;

import com.incidentpilot.client.HindsightClient;
import com.incidentpilot.client.LLMClient;
import com.incidentpilot.dto.IncidentAnalysisRequest;
import com.incidentpilot.dto.IncidentAnalysisResponse;
import com.incidentpilot.dto.PostmortemRequest;
import com.incidentpilot.dto.PostmortemResponse;
import com.incidentpilot.service.IncidentService;
import com.incidentpilot.service.PostmortemService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HindsightClientTest {

    @Test
    void testMissingApiKey_FallbackMode() {
        // When HINDSIGHT_API_KEY is not configured
        HindsightClient client = new HindsightClient(
                WebClient.builder(),
                "https://api.hindsight.vectorize.io",
                "",
                "incidentpilot",
                "",
                "",
                "",
                10
        );

        assertFalse(client.isCloudConfigured(), "Should be in fallback mode when API key is missing");
        assertEquals("incidentpilot", client.getBankId());

        // Retain works via inMemoryStore
        String retentionId = client.retain(
                "Checkout API",
                "INC-TEST-001",
                "High latency",
                "Connection pool leak",
                "Session closure",
                List.of("Increasing pool"),
                "Leak detection",
                List.of("Always close sessions")
        );
        assertNotNull(retentionId);
        assertTrue(retentionId.startsWith("hindsight_mem_"));

        // Recall works via inMemoryStore
        List<String> recalled = client.recall("Checkout API", "High latency", "", "SEV-1");
        assertFalse(recalled.isEmpty(), "Should recall retained memory from in-memory fallback");
        assertTrue(recalled.get(0).contains("Connection pool leak"));

        // Reflect works via inMemoryStore
        String reflection = client.reflect(Map.of("service", "Checkout API"));
        assertNotNull(reflection);
        assertTrue(reflection.contains("Reflection completed"));
    }

    @Test
    void testHindsightClient_Retain_Recall_Reflect_WithMockCloudApi() {
        // Mock Hindsight Cloud endpoints according to OpenAPI specification
        ExchangeFunction mockExchange = request -> {
            String path = request.url().getPath();
            HttpHeaders headers = request.headers();

            // Verify authentication header
            assertEquals("Bearer mock-key", headers.getFirst(HttpHeaders.AUTHORIZATION));

            if (path.endsWith("/memories/recall")) {
                // Return RecallResponse matching OpenAPI schema
                String json = "{\"results\":[{\"id\":\"mem-1\",\"text\":\"Previous Checkout API incident was caused by database connection leak.\",\"type\":\"world\"}]}";
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            } else if (path.endsWith("/memories")) {
                // Return RetainResponse matching OpenAPI schema
                String json = "{\"success\":true,\"bank_id\":\"incidentpilot\",\"items_count\":1,\"async\":false}";
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            } else if (path.endsWith("/reflect")) {
                // Return ReflectResponse matching OpenAPI schema
                String json = "{\"text\":\"Synthesized lesson: Ensure database connections are released in finally blocks.\"}";
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());
            }
            return Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
        };

        WebClient.Builder mockBuilder = WebClient.builder().exchangeFunction(mockExchange);

        HindsightClient client = new HindsightClient(
                mockBuilder,
                "https://api.hindsight.vectorize.io",
                "mock-key",
                "incidentpilot",
                "",
                "",
                "",
                10
        );

        assertTrue(client.isCloudConfigured());

        // 1. Retain
        String retId = client.retain(
                "Checkout API",
                "INC-001",
                "DB pool exhaustion",
                "Connection leak",
                "Fixed lifecycle",
                List.of("Increased pool size"),
                "Monitoring",
                List.of("Close connections")
        );
        assertNotNull(retId);

        // 2. Recall
        List<String> memories = client.recall("Checkout API", "High latency", "", "SEV-1");
        assertFalse(memories.isEmpty());
        assertTrue(memories.stream().anyMatch(m -> m.contains("database connection leak")));

        // 3. Reflect
        String reflectAnswer = client.reflect(Map.of("service", "Checkout API", "incident", "INC-001"));
        assertNotNull(reflectAnswer);
        assertTrue(reflectAnswer.contains("Ensure database connections are released"));
    }

    @Test
    void testHindsightHttpFailure_GracefulFallback() {
        // When Cloud API returns 500 Internal Server Error
        ExchangeFunction errorExchange = request -> Mono.just(
                ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"error\":\"Internal Server Error\"}")
                        .build()
        );

        WebClient.Builder errorBuilder = WebClient.builder().exchangeFunction(errorExchange);

        HindsightClient client = new HindsightClient(
                errorBuilder,
                "https://api.hindsight.vectorize.io",
                "mock-key",
                "incidentpilot",
                "",
                "",
                "",
                10
        );

        // Retain should not throw exception; it falls back to inMemoryStore
        assertDoesNotThrow(() -> {
            String retId = client.retain(
                    "Payments API",
                    "INC-FAIL-01",
                    "Gateway timeout",
                    "Downstream latency",
                    "Circuit breaker",
                    Collections.emptyList(),
                    "Timeout tuning",
                    Collections.emptyList()
            );
            assertNotNull(retId);
        });

        // Recall should not throw exception; it consults local memory
        assertDoesNotThrow(() -> {
            List<String> memories = client.recall("Payments API", "Gateway timeout", "", "SEV-1");
            assertNotNull(memories);
            assertFalse(memories.isEmpty(), "Should fall back to in-memory store when Cloud API fails");
            assertTrue(memories.get(0).contains("Downstream latency"));
        });

        // Reflect should not throw exception; it returns fallback summary
        assertDoesNotThrow(() -> {
            String answer = client.reflect(Map.of("service", "Payments API"));
            assertNotNull(answer);
            assertTrue(answer.contains("Reflection completed"));
        });
    }

    @Test
    void testIncidentToRecallToLlmContextFlow() {
        HindsightClient hindsightClient = new HindsightClient(
                WebClient.builder(),
                "https://api.hindsight.vectorize.io",
                "",
                "incidentpilot",
                "",
                "",
                "",
                10
        );

        // Seed an incident into memory
        hindsightClient.retain(
                "Orders API",
                "INC-ORD-1",
                "Connection timeout",
                "Stuck transactions",
                "Added transaction timeout",
                List.of("Restarting container"),
                "Alerting",
                List.of("Set statement timeouts")
        );

        LLMClient llmClient = new LLMClient(WebClient.builder(), "https://api.openai.com/v1", "", "gpt-4o-mini");
        IncidentService incidentService = new IncidentService(hindsightClient, llmClient);

        // Analyze similar incident
        IncidentAnalysisRequest request = IncidentAnalysisRequest.builder()
                .serviceName("Orders API")
                .symptoms("Connection timeout on checkout")
                .severity("SEV-1")
                .build();

        IncidentAnalysisResponse response = incidentService.analyze(request);
        assertNotNull(response);

        // Recalled memory must be passed to the diagnosis
        assertFalse(response.getHistoricalIncidents().isEmpty());
        assertTrue(response.getHistoricalIncidents().stream().anyMatch(h -> h.contains("Stuck transactions")));

        // Recommended fixes dynamically incorporate recalled fix and warn against failed approach
        assertTrue(response.getRecommendedFixes().stream().anyMatch(f -> f.toLowerCase().contains("transaction timeout")));
    }

    @Test
    void testPostmortemToRetainToReflectFlow() {
        HindsightClient hindsightClient = new HindsightClient(
                WebClient.builder(),
                "https://api.hindsight.vectorize.io",
                "",
                "incidentpilot",
                "",
                "",
                "",
                10
        );

        PostmortemService postmortemService = new PostmortemService(hindsightClient);

        PostmortemRequest request = PostmortemRequest.builder()
                .incidentId("INC-TEST-POSTMORTEM")
                .serviceName("Search API")
                .symptoms("CPU 100% under high query volume")
                .actualRootCause("Regex catastrophic backtracking on wildcards")
                .successfulFix("Replaced regex with prefix trie index")
                .failedApproaches("Doubling CPU cores")
                .preventionStrategy("Static lint rule against non-anchored wildcards")
                .lessonsLearned("Avoid complex regex in user search queries")
                .build();

        PostmortemResponse response = postmortemService.processPostmortem(request);
        assertNotNull(response);
        assertEquals("INC-TEST-POSTMORTEM", response.getIncidentId());
        assertEquals("SUCCESS", response.getRetentionStatus());
        assertEquals("SUCCESS", response.getReflectionStatus());
        assertEquals("COMPLETED", response.getStatus());

        // Verify that subsequent recall immediately finds it
        List<String> memories = hindsightClient.recall("Search API", "CPU spike", "", "SEV-1");
        assertFalse(memories.isEmpty());
        assertTrue(memories.get(0).contains("catastrophic backtracking"));
    }
}
