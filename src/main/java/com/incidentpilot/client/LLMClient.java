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
 * LLM Client for IncidentPilot incident analysis and diagnosis.
 * 
 * STEP 4 & 5:
 * Builds prompt containing CURRENT INCIDENT + HISTORICAL MEMORY.
 * 
 * STRICT CONSTRAINTS:
 * - Must NOT execute commands.
 * - Must NOT claim that it performed a remediation.
 * - Must NOT fabricate historical information.
 * - If Hindsight returns [], tells LLM: "No relevant historical memory was found."
 * - When historical memory is provided, explicitly incorporates it into recommendations.
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
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = new ObjectMapper();
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
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

        log.debug("Building LLM prompt and calling LLM for service='{}', severity='{}'", service, severity);

        boolean hasHistoricalMemory = (recalledMemories != null && !recalledMemories.isEmpty());

        // STEP 4: Format HISTORICAL MEMORY
        // If Hindsight returns [], tell the LLM: "No relevant historical memory was found."
        // If Hindsight returns a previous incident, include it in the LLM context.
        String historicalMemoryText;
        if (!hasHistoricalMemory) {
            historicalMemoryText = "No relevant historical memory was found.";
        } else {
            StringBuilder sb = new StringBuilder();
            for (String memory : recalledMemories) {
                sb.append("- ").append(memory).append("\n");
            }
            historicalMemoryText = sb.toString().trim();
        }

        // System prompt enforcing safety and output schema
        String systemPrompt = "You are IncidentPilot, an expert Site Reliability Engineering (SRE) diagnostic AI.\n\n" +
                "CORE RESPONSIBILITIES:\n" +
                "1. Analyze the CURRENT INCIDENT alongside HISTORICAL MEMORY from past incidents.\n" +
                "2. Explicitly reflect past learnings:\n" +
                "   - If historical memory contains a previous incident, root cause, or successful fix, prioritize those learnings in possibleRootCauses and recommendedFixes.\n" +
                "   - If historical memory contains failed approaches, explicitly advise against repeating those failed approaches (e.g. 'Check X before Y, which previously failed').\n" +
                "   - In memoryBasedInsights, make it obvious whether and how historical memory influenced your recommendations.\n" +
                "   - If HISTORICAL MEMORY states 'No relevant historical memory was found.', state clearly in memoryBasedInsights that no historical incident memory was available and this diagnosis is derived solely from current incident symptoms and logs.\n" +
                "   - In historicalIncidents, list the relevant past incidents from memory, or set to ['No relevant historical incidents found.'] if no memory was found. DO NOT fabricate incidents.\n\n" +
                "STRICT SAFETY & OPERATIONAL RULES:\n" +
                "1. You must NOT execute commands or run scripts.\n" +
                "2. You must NOT claim or state that you performed any remediation. All suggestions are recommendations for engineers.\n" +
                "3. Output MUST be a single valid JSON object adhering strictly to the schema.";

        // User prompt structured with CURRENT INCIDENT + HISTORICAL MEMORY
        String userPrompt = String.format(
                "CURRENT INCIDENT:\n" +
                "- service: %s\n" +
                "- symptoms: %s\n" +
                "- logs: %s\n" +
                "- severity: %s\n\n" +
                "HISTORICAL MEMORY:\n" +
                "%s\n\n" +
                "Analyze the incident and return a JSON object with exactly these keys:\n" +
                "{\n" +
                "  \"possibleRootCauses\": [\"...\"],\n" +
                "  \"investigationSteps\": [\"...\"],\n" +
                "  \"recommendedFixes\": [\"...\"],\n" +
                "  \"historicalIncidents\": [\"...\"],\n" +
                "  \"memoryBasedInsights\": [\"...\"]\n" +
                "}",
                formatValue(service),
                formatValue(symptoms),
                formatValue(logs),
                formatValue(severity),
                historicalMemoryText
        );

        // Prototype / Fallback mode when API key is not configured
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("LLM_API_KEY not configured. Generating prototype diagnosis from actual recalled memory.");
            return generateFallbackResponse(service, severity, symptoms, logs, recalledMemories);
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.1);
            // Structured JSON output
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
            log.warn("Unexpected LLM response structure. Falling back to dynamic diagnosis.");
            return generateFallbackResponse(service, severity, symptoms, logs, recalledMemories);
        } catch (Exception e) {
            log.error("LLM call failed: {}. Falling back to dynamic diagnosis.", e.getMessage());
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
     * Legacy Workflow 1 string diagnosis.
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
     * Safely parse JSON returned by LLM.
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

            List<String> rootCauses = extractList(root, "possibleRootCauses");
            List<String> steps = extractList(root, "investigationSteps");
            List<String> fixes = extractList(root, "recommendedFixes");
            List<String> history = extractList(root, "historicalIncidents");
            List<String> insights = extractList(root, "memoryBasedInsights");

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
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON text: '{}'. Falling back to dynamic diagnosis.", jsonText);
            return generateFallbackResponse(service, severity, symptoms, logs, recalledMemories);
        }
    }

    /**
     * Dynamic SRE diagnostic reasoning based on the actual recalled memory:
     * - Generates recommendations from the actual memory without hardcoding.
     * - If memory has previous fix and failed approach, highlights both in recommendation.
     * - If memory is empty, states clearly that no relevant historical memory was found.
     * - Does NOT execute commands.
     * - Does NOT claim remediation has been performed.
     * - Does NOT fabricate historical incidents.
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

        if (!hasMemory) {
            historicalIncidents.add("No relevant historical incidents found.");
            memoryBasedInsights.add("No relevant historical memory was found. Diagnosis is based solely on current incident symptoms and logs without historical precedent.");
        } else {
            historicalIncidents.addAll(recalledMemories);

            // Dynamically derive recommendations from the actual recalled memories
            for (String memory : recalledMemories) {
                String successfulFix = extractPattern(memory, "successful fix:", "fix:", "resolved by:");
                String failedApproach = extractPattern(memory, "failed approach:", "failed attempt:", "failed:");
                String rootCause = extractPattern(memory, "caused by", "root cause:", "cause:");
                String preventionLesson = extractPattern(memory, "lessons learned:", "prevention lessons:", "prevention:", "lesson:");

                if (rootCause != null && !rootCause.isBlank()) {
                    rootCauses.add("Historical pattern: " + rootCause);
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

        // Augment with symptoms/telemetry analysis
        String combined = ((symptoms != null ? symptoms : "") + " " + (logs != null ? logs : "")).toLowerCase();
        if (combined.contains("database") || combined.contains("timeout") || combined.contains("connection") || combined.contains("hikaripool")) {
            if (rootCauses.isEmpty()) {
                rootCauses.add("Database connection pool exhaustion or high contention under load");
                rootCauses.add("Downstream database unresponsiveness or slow queries holding transactions");
            }
            investigationSteps.add("Check active versus idle connection pool metrics (e.g. HikariCP pool usage)");
            investigationSteps.add("Inspect database slow query log and active lock waits");
            investigationSteps.add("Verify network connectivity and latency between " + service + " and database cluster");

            if (recommendedFixes.isEmpty()) {
                recommendedFixes.add("Recommendation: Temporarily scale up connection pool maximum size if database capacity permits");
                recommendedFixes.add("Recommendation: Terminate stuck long-running queries holding locks");
            }
        } else if (combined.contains("certificate") || combined.contains("ssl") || combined.contains("tls") || combined.contains("handshake") || combined.contains("auth")) {
            if (rootCauses.isEmpty()) {
                rootCauses.add("TLS/SSL certificate expiration or validation failure");
                rootCauses.add("Authentication provider or identity service unreachable");
            }
            investigationSteps.add("Inspect TLS/SSL certificate validity and expiration dates across endpoints");
            investigationSteps.add("Verify identity provider trust store and keystore configurations");
            investigationSteps.add("Check auth gateway connectivity and token validation logs");

            if (recommendedFixes.isEmpty()) {
                recommendedFixes.add("Recommendation: Renew and rotate expired TLS/SSL certificates");
                recommendedFixes.add("Recommendation: Verify certificate authorities and truststore entries");
            }
        } else {
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

        return IncidentAnalysisResponse.builder()
                .serviceName(service)
                .severity(severity)
                .possibleRootCauses(rootCauses)
                .investigationSteps(investigationSteps)
                .recommendedFixes(recommendedFixes)
                .historicalIncidents(historicalIncidents)
                .memoryBasedInsights(memoryBasedInsights)
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

    private List<String> extractList(JsonNode root, String fieldName) {
        if (root != null && root.has(fieldName) && root.get(fieldName).isArray()) {
            List<String> list = new ArrayList<>();
            for (JsonNode node : root.get(fieldName)) {
                list.add(node.asText());
            }
            return list;
        }
        return Collections.emptyList();
    }

    private String formatValue(String val) {
        return (val != null && !val.isBlank()) ? val : "N/A";
    }
}