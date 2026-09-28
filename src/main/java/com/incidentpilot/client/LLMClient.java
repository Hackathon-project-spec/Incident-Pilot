package com.incidentpilot.client;

import com.incidentpilot.dto.IncidentAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM Client for IncidentPilot incident analysis, diagnosis, and memory comparison.
 * 
 * CORE RESPONSIBILITIES (Person 3):
 * 1. Combines CURRENT INCIDENT + HISTORICAL MEMORY (from Hindsight) into high-signal prompts.
 * 2. Formats structured diagnosis output:
 *    - root cause hypothesis (possibleRootCauses)
 *    - investigation steps (investigationSteps)
 *    - possible fixes (recommendedFixes)
 *    - related past incidents (historicalIncidents)
 *    - failed approaches to avoid (failedApproachesToAvoid)
 *    - memory-based insights (memoryBasedInsights)
 * 3. Enforces strict SRE guardrails:
 *    - Never executes destructive commands.
 *    - Never claims remediation has already been performed.
 *    - Never fabricates historical incidents.
 *    - Honestly reports when memory is absent ("No relevant historical memory was found.").
 * 4. Resilient local fallback reasoning when LLM API keys are unconfigured.
 */
@Slf4j
@Component
public class LLMClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;

    public LLMClient(
            WebClient.Builder webClientBuilder,
            @Value("${llm.base.url:${LLM_BASE_URL:https://api.openai.com/v1}}") String baseUrl,
            @Value("${llm.api.key:${LLM_API_KEY:}}") String apiKey,
            @Value("${llm.model:${LLM_MODEL:gpt-4o-mini}}") String model) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "gpt-4o-mini";
        this.objectMapper = new ObjectMapper();
        this.webClient = webClientBuilder
                .baseUrl(baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Complete Incident Analysis:
     * Receives CURRENT INCIDENT and HISTORICAL MEMORY from Hindsight recall.
     */
    public IncidentAnalysisResponse analyzeIncident(
            String service,
            String symptoms,
            String logs,
            String severity,
            List<String> recalledMemories) {

        log.debug("Building LLM prompt for service='{}', severity='{}', memoryCount={}", 
                service, severity, (recalledMemories != null ? recalledMemories.size() : 0));

        boolean hasHistoricalMemory = (recalledMemories != null && !recalledMemories.isEmpty()
                && !recalledMemories.contains("No relevant historical incidents found."));

        // STEP 4: Format HISTORICAL MEMORY context
        String historicalMemoryText;
        if (!hasHistoricalMemory) {
            historicalMemoryText = "No relevant historical memory was found.";
        } else {
            StringBuilder sb = new StringBuilder();
            for (String memory : recalledMemories) {
                if (memory != null && !memory.isBlank()) {
                    sb.append("- ").append(memory.trim()).append("\n");
                }
            }
            historicalMemoryText = sb.length() > 0 ? sb.toString().trim() : "No relevant historical memory was found.";
        }

        // Tuned SRE System Prompt
        String systemPrompt = "You are IncidentPilot, an expert Site Reliability Engineering (SRE) diagnostic AI assistant.\n\n" +
                "PRIMARY OBJECTIVE:\n" +
                "Diagnose live system incidents with high precision by synthesizing CURRENT INCIDENT telemetry with HISTORICAL MEMORY from past postmortems.\n\n" +
                "DIAGNOSIS GUIDELINES:\n" +
                "1. If HISTORICAL MEMORY contains past incidents, root causes, or proven fixes:\n" +
                "   - Prioritize those verified learnings in 'possibleRootCauses' and 'recommendedFixes'.\n" +
                "   - Populate 'failedApproachesToAvoid' with past failed attempts and explicitly instruct engineers why they failed (e.g. 'Do NOT increase connection pool size; in INC-DEMO-001 this masked the leak and exhausted DB resources').\n" +
                "   - Populate 'historicalIncidents' with the matching past incidents.\n" +
                "   - Detail in 'memoryBasedInsights' exactly how the recalled memory altered this diagnosis compared to a blind cold-start triage.\n" +
                "   - Set 'confidenceScore' to 'HIGH (Memory-Correlated)'.\n\n" +
                "2. If HISTORICAL MEMORY states 'No relevant historical memory was found.' or is empty:\n" +
                "   - Perform first-principles telemetry diagnosis based strictly on the provided symptoms and logs.\n" +
                "   - In 'historicalIncidents', return exactly [\"No relevant historical incidents found.\"].\n" +
                "   - In 'memoryBasedInsights', state clearly: \"No relevant historical memory was found. Diagnosis is derived solely from current incident symptoms and logs without historical precedent.\"\n" +
                "   - Set 'confidenceScore' to 'MEDIUM (First Principles)'.\n" +
                "   - DO NOT fabricate past incidents, fake ticket IDs, or non-existent history.\n\n" +
                "STRICT SAFETY RULES:\n" +
                "1. You are an advisory AI. NEVER execute terminal commands or run scripts.\n" +
                "2. NEVER claim or state that you performed a remediation (e.g. do not say 'I restarted the pods'). All recommendations are actionable suggestions for on-call engineers.\n" +
                "3. Output MUST be a single valid JSON object adhering strictly to the required schema.";

        // Structured User Prompt
        String userPrompt = String.format(
                "=== CURRENT INCIDENT TELEMETRY ===\n" +
                "- Service: %s\n" +
                "- Severity: %s\n" +
                "- Symptoms: %s\n" +
                "- Telemetry / Logs: %s\n\n" +
                "=== HISTORICAL MEMORY CONTEXT ===\n" +
                "%s\n\n" +
                "Return a JSON object adhering to this exact schema:\n" +
                "{\n" +
                "  \"possibleRootCauses\": [\"Hypothesis 1 with technical reasoning\", \"Hypothesis 2...\"],\n" +
                "  \"investigationSteps\": [\"Step 1: Check metrics...\", \"Step 2: Inspect logs...\"],\n" +
                "  \"recommendedFixes\": [\"Primary mitigation action...\", \"Alternative action...\"],\n" +
                "  \"failedApproachesToAvoid\": [\"Anti-pattern or past failed approach to avoid...\"],\n" +
                "  \"historicalIncidents\": [\"Incident ID / description or 'No relevant historical incidents found.'\"],\n" +
                "  \"memoryBasedInsights\": [\"Detailed explanation of memory influence...\"],\n" +
                "  \"confidenceScore\": \"HIGH (Memory-Correlated) | MEDIUM (First Principles) | LOW (Insufficient Data)\"\n" +
                "}",
                formatValue(service),
                formatValue(severity),
                formatValue(symptoms),
                formatValue(logs),
                historicalMemoryText
        );

        // Resilient Fallback if API key is not configured
        if (apiKey.isBlank()) {
            log.debug("LLM_API_KEY not configured. Generating prototype SRE diagnosis from actual recalled memory.");
            return generateFallbackResponse(service, severity, symptoms, logs, recalledMemories);
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.1);
            requestBody.put("response_format", Map.of("type", "json_object"));
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));

            Map<?, ?> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<?> choices = (List<?>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                    if (message != null && message.containsKey("content")) {
                        String content = (String) message.get("content");
                        return parseJsonResponseSafely(content, service, severity, symptoms, logs, recalledMemories);
                    }
                }
            }
            log.warn("Unexpected LLM response structure. Falling back to dynamic SRE reasoning.");
            return generateFallbackResponse(service, severity, symptoms, logs, recalledMemories);
        } catch (Exception e) {
            log.error("LLM call failed ({}). Falling back to dynamic SRE reasoning.", e.getMessage());
            return generateFallbackResponse(service, severity, symptoms, logs, recalledMemories);
        }
    }

    /**
     * Overloaded analyze method.
     */
    public IncidentAnalysisResponse analyze(
            String service,
            String symptoms,
            String logs,
            String severity,
            List<String> historicalIncidents) {
        return analyzeIncident(service, symptoms, logs, severity, historicalIncidents);
    }

    /**
     * Overloaded analyze method with individual lists.
     */
    public IncidentAnalysisResponse analyze(
            String service,
            String symptoms,
            String logs,
            String severity,
            List<String> previousSimilarIncidents,
            List<String> previousRootCauses,
            List<String> successfulFixes,
            List<String> failedApproaches,
            List<String> preventionLessons) {

        List<String> combinedMemories = new ArrayList<>();
        if (previousSimilarIncidents != null) combinedMemories.addAll(previousSimilarIncidents);
        if (previousRootCauses != null && !previousRootCauses.isEmpty()) {
            combinedMemories.add("Previous root causes: " + String.join(", ", previousRootCauses));
        }
        if (successfulFixes != null && !successfulFixes.isEmpty()) {
            combinedMemories.add("Previous successful fix: " + String.join(", ", successfulFixes));
        }
        if (failedApproaches != null && !failedApproaches.isEmpty()) {
            combinedMemories.add("Previous failed approach: " + String.join(", ", failedApproaches));
        }
        if (preventionLessons != null && !preventionLessons.isEmpty()) {
            combinedMemories.add("Prevention lessons: " + String.join(", ", preventionLessons));
        }

        return analyzeIncident(service, symptoms, logs, severity, combinedMemories);
    }

    /**
     * Legacy string diagnosis method.
     */
    public String generateDiagnosis(String incidentDetails, List<String> recalledMemories) {
        IncidentAnalysisResponse response = analyzeIncident(
                "Service",
                incidentDetails,
                "",
                "UNKNOWN",
                recalledMemories
        );
        return String.format(
                "Diagnosis Summary:\n- Possible Root Causes: %s\n- Recommended Fixes: %s",
                String.join(", ", response.getPossibleRootCauses()),
                String.join(", ", response.getRecommendedFixes())
        );
    }

    /**
     * Safely parse JSON returned by LLM with field mapping & fault tolerance.
     */
    private IncidentAnalysisResponse parseJsonResponseSafely(
            String jsonText,
            String service,
            String severity,
            String symptoms,
            String logs,
            List<String> recalledMemories) {
        try {
            String cleaned = jsonText.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            JsonNode root = objectMapper.readTree(cleaned);

            List<String> rootCauses = extractList(root, "possibleRootCauses", "rootCauseHypothesis", "rootCauses");
            List<String> steps = extractList(root, "investigationSteps", "steps", "investigation");
            List<String> fixes = extractList(root, "recommendedFixes", "possibleFixes", "fixes", "mitigations");
            List<String> history = extractList(root, "historicalIncidents", "relatedPastIncidents", "history");
            List<String> insights = extractList(root, "memoryBasedInsights", "insights", "memoryInsights");
            List<String> failedApproaches = extractList(root, "failedApproachesToAvoid", "failedApproaches", "avoid");

            String confidence = root.has("confidenceScore") ? root.get("confidenceScore").asText() : null;

            if (history.isEmpty()) {
                history = (recalledMemories != null && !recalledMemories.isEmpty())
                        ? recalledMemories
                        : List.of("No relevant historical incidents found.");
            }

            return IncidentAnalysisResponse.builder()
                    .serviceName(service)
                    .severity(severity)
                    .possibleRootCauses(rootCauses.isEmpty() ? List.of("Unspecified operational anomaly") : rootCauses)
                    .investigationSteps(steps.isEmpty() ? List.of("Inspect active service metrics and logs") : steps)
                    .recommendedFixes(fixes.isEmpty() ? List.of("Verify container health and configuration") : fixes)
                    .historicalIncidents(history)
                    .memoryBasedInsights(insights.isEmpty() ? List.of("Analysis completed based on telemetry.") : insights)
                    .failedApproachesToAvoid(failedApproaches)
                    .confidenceScore(confidence != null ? confidence : (recalledMemories != null && !recalledMemories.isEmpty() ? "HIGH (Memory-Correlated)" : "MEDIUM (First Principles)"))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON text: '{}'. Falling back to dynamic SRE reasoning.", jsonText);
            return generateFallbackResponse(service, severity, symptoms, logs, recalledMemories);
        }
    }

    /**
     * Dynamic SRE diagnostic reasoning based on the actual recalled memory:
     * - Generates recommendations dynamically from actual memory without hardcoding.
     * - Extracts successful fixes, failed approaches, root causes, and prevention strategies.
     * - If memory is empty, states clearly that no relevant historical memory was found.
     * - Strictly adheres to safety constraints: no commands, no remediation claims, no fabricated history.
     */
    private IncidentAnalysisResponse generateFallbackResponse(
            String service,
            String severity,
            String symptoms,
            String logs,
            List<String> recalledMemories) {

        boolean hasMemory = (recalledMemories != null && !recalledMemories.isEmpty()
                && !recalledMemories.contains("No relevant historical incidents found."));

        List<String> rootCauses = new ArrayList<>();
        List<String> investigationSteps = new ArrayList<>();
        List<String> recommendedFixes = new ArrayList<>();
        List<String> historicalIncidents = new ArrayList<>();
        List<String> memoryBasedInsights = new ArrayList<>();
        List<String> failedApproachesToAvoid = new ArrayList<>();

        if (!hasMemory) {
            historicalIncidents.add("No relevant historical incidents found.");
            memoryBasedInsights.add("No relevant historical memory was found. Diagnosis is based solely on current incident symptoms and logs without historical precedent.");
        } else {
            historicalIncidents.addAll(recalledMemories);

            // Dynamically derive recommendations from the actual recalled memories
            for (String memory : recalledMemories) {
                String successfulFix = extractPattern(memory, "successful fix:", "fix:", "resolved by:", "proven fix:");
                String failedApproach = extractPattern(memory, "failed approach:", "failed attempt:", "failed:", "failed approaches:");
                String rootCause = extractPattern(memory, "caused by:", "caused by", "root cause:", "cause:", "actual root cause:");
                String preventionLesson = extractPattern(memory, "lessons learned:", "prevention lessons:", "prevention:", "lesson:", "prevention strategy:");

                if (rootCause != null && !rootCause.isBlank()) {
                    rootCauses.add("Historical pattern: " + rootCause);
                }

                if (failedApproach != null && !failedApproach.isBlank()) {
                    failedApproachesToAvoid.add("Avoid: " + failedApproach + " (recorded as an ineffective/counter-productive approach in past incident)");
                }

                if (successfulFix != null && failedApproach != null) {
                    recommendedFixes.add(String.format(
                            "Check %s before %s (which previously failed in historical memory).",
                            successfulFix.toLowerCase(), failedApproach.toLowerCase()));
                    memoryBasedInsights.add(String.format(
                            "Historical memory directly influenced recommendation: Prioritizing proven fix ('%s') and cautioning against previously failed approach ('%s').",
                            successfulFix, failedApproach));
                } else if (successfulFix != null) {
                    recommendedFixes.add("Apply proven mitigation from past incident: " + successfulFix);
                    memoryBasedInsights.add("Historical memory directly influenced recommendation: Past resolution identified ('" + successfulFix + "').");
                } else if (failedApproach != null) {
                    recommendedFixes.add("Caution: Avoid " + failedApproach + " as historical memory recorded it as an unsuccessful approach.");
                    memoryBasedInsights.add("Historical memory warning: Past failed attempt noted ('" + failedApproach + "').");
                } else {
                    memoryBasedInsights.add("Historical memory consulted: Correlated with past event pattern ('" + memory + "').");
                }

                if (preventionLesson != null && !preventionLesson.isBlank()) {
                    memoryBasedInsights.add("Historical lesson applied: " + preventionLesson);
                }
            }
        }

        // Augment with telemetry & domain pattern recognition
        String combined = ((symptoms != null ? symptoms : "") + " " + (logs != null ? logs : "")).toLowerCase();
        
        // Scenario 1: Connection pool & database timeouts
        if (combined.contains("database") || combined.contains("timeout") || combined.contains("connection") || combined.contains("hikaripool") || combined.contains("leak")) {
            if (rootCauses.isEmpty()) {
                rootCauses.add("Database connection pool exhaustion or high contention under load");
                rootCauses.add("Downstream database unresponsiveness or slow queries holding transactions");
            }
            investigationSteps.add("Check active versus idle connection pool metrics (e.g. HikariCP active connections / wait time)");
            investigationSteps.add("Inspect database slow query log and active lock waits");
            investigationSteps.add("Verify connection lifecycle handling in application database access layer for unclosed connections");
            investigationSteps.add("Verify network connectivity and latency between " + service + " and database cluster");

            if (recommendedFixes.isEmpty()) {
                recommendedFixes.add("Inspect connection lifecycle and ensure all database sessions are wrapped in try-with-resources");
                recommendedFixes.add("Recommendation: Temporarily scale up connection pool maximum size if database capacity permits");
                recommendedFixes.add("Recommendation: Terminate stuck long-running queries holding locks");
            }
        } 
        // Scenario 2: Auth / Certificate / JWT / Token errors
        else if (combined.contains("certificate") || combined.contains("ssl") || combined.contains("tls") || combined.contains("handshake") || combined.contains("auth") || combined.contains("jwt") || combined.contains("401") || combined.contains("jwks")) {
            if (rootCauses.isEmpty()) {
                rootCauses.add("TLS/SSL certificate expiration or validation failure");
                rootCauses.add("Authentication provider JWKS key rotation cache mismatch or token validation failure");
            }
            investigationSteps.add("Inspect TLS/SSL certificate validity and expiration dates across endpoints");
            investigationSteps.add("Verify identity provider trust store, JWKS public key cache, and token signing configurations");
            investigationSteps.add("Check auth gateway connectivity and token validation logs");

            if (recommendedFixes.isEmpty()) {
                recommendedFixes.add("Recommendation: Evict stale JWKS key cache and reload public signing keys");
                recommendedFixes.add("Recommendation: Renew and rotate expired TLS/SSL certificates");
            }
        } 
        // Scenario 3: Kafka / Consumer Lag / Poison Pill
        else if (combined.contains("kafka") || combined.contains("consumer") || combined.contains("lag") || combined.contains("poison") || combined.contains("deserialization")) {
            if (rootCauses.isEmpty()) {
                rootCauses.add("Poison pill deserialization error causing infinite consumer retry loop");
                rootCauses.add("Consumer partition rebalancing loop or processing thread starvation");
            }
            investigationSteps.add("Inspect Kafka consumer lag per partition and consumer group status");
            investigationSteps.add("Check dead-letter queue (DLQ) metrics and deserialization exception stack traces");
            investigationSteps.add("Verify offset commit behaviour under error conditions");

            if (recommendedFixes.isEmpty()) {
                recommendedFixes.add("Recommendation: Route unparseable poison pill messages to Dead Letter Queue (DLQ)");
                recommendedFixes.add("Recommendation: Review producer schema serialization compatibility");
            }
        }
        // Scenario 4: Thread Pool / Deadlock / Execution Rejection
        else if (combined.contains("thread") || combined.contains("rejectedexecution") || combined.contains("deadlock") || combined.contains("worker")) {
            if (rootCauses.isEmpty()) {
                rootCauses.add("Worker thread pool exhaustion caused by blocking synchronous downstream I/O");
                rootCauses.add("Thread deadlock on shared resource synchronization");
            }
            investigationSteps.add("Capture and analyze JVM thread dumps (jstack) to identify thread states (BLOCKED/WAITING)");
            investigationSteps.add("Inspect executor queue depth and task rejection rate metrics");
            investigationSteps.add("Trace downstream HTTP/RPC call timeouts within async tasks");

            if (recommendedFixes.isEmpty()) {
                recommendedFixes.add("Recommendation: Convert blocking downstream synchronous calls to non-blocking asynchronous clients with timeouts");
                recommendedFixes.add("Recommendation: Apply circuit breaker to prevent cascading thread pool saturation");
            }
        }
        // Scenario 5: General resource saturation
        else {
            if (rootCauses.isEmpty()) {
                rootCauses.add("Service resource saturation (CPU or memory starvation)");
                rootCauses.add("Cascading downstream service dependency latency");
            }
            investigationSteps.add("Inspect CPU and memory utilization graphs for " + service);
            investigationSteps.add("Trace upstream and downstream HTTP/RPC call latencies");

            if (recommendedFixes.isEmpty()) {
                recommendedFixes.add("Recommendation: Horizontally scale service replicas for " + service);
                recommendedFixes.add("Recommendation: Review recent deployment diffs or configuration changes");
            }
        }

        String confidence = hasMemory ? "HIGH (Memory-Correlated)" : "MEDIUM (First Principles)";

        return IncidentAnalysisResponse.builder()
                .serviceName(service)
                .severity(severity)
                .possibleRootCauses(rootCauses)
                .investigationSteps(investigationSteps)
                .recommendedFixes(recommendedFixes)
                .historicalIncidents(historicalIncidents)
                .memoryBasedInsights(memoryBasedInsights)
                .failedApproachesToAvoid(failedApproachesToAvoid)
                .confidenceScore(confidence)
                .build();
    }

    private String extractPattern(String text, String... markers) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        for (String marker : markers) {
            int idx = lower.indexOf(marker.toLowerCase());
            if (idx != -1) {
                int start = idx + marker.length();
                while (start < text.length() && (text.charAt(start) == ':' || text.charAt(start) == ' ' || text.charAt(start) == '-')) {
                    start++;
                }
                int end = text.indexOf(".", start);
                if (end == -1) end = text.indexOf(";", start);
                if (end == -1) end = text.indexOf("\n", start);
                if (end == -1) end = text.length();

                String extracted = text.substring(start, end).trim();
                if (!extracted.isEmpty()) {
                    return extracted;
                }
            }
        }
        return null;
    }

    private List<String> extractList(JsonNode root, String... fieldNames) {
        if (root != null) {
            for (String fieldName : fieldNames) {
                if (root.has(fieldName) && root.get(fieldName).isArray()) {
                    List<String> list = new ArrayList<>();
                    for (JsonNode node : root.get(fieldName)) {
                        list.add(node.asText());
                    }
                    return list;
                }
            }
        }
        return Collections.emptyList();
    }

    private String formatValue(String val) {
        return (val != null && !val.isBlank()) ? val : "N/A";
    }
}