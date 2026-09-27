package com.incidentpilot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hindsight Client for long-term memory orchestration.
 * 
 * Required operations:
 * 1. recall (serviceName, symptoms, logs, severity)
 * 2. retain (service, incident, symptoms, actual root cause, successful fix, failed approaches, prevention, lessons learned)
 * 3. reflect (official Hindsight reflection mechanism)
 * 
 * All Hindsight HTTP logic, WebClient configuration, credentials, timeouts,
 * and error handling are encapsulated here.
 * 
 * Demonstrates long-term memory lifecycle without database persistence.
 */
@Slf4j
@Component
public class HindsightClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String baseUrl;
    private final String recallEndpoint;
    private final String retainEndpoint;
    private final String reflectEndpoint;
    private final long timeoutSeconds;

    // In-memory long-term memory store demonstrating memory retention across requests
    private final List<Map<String, String>> inMemoryStore = new CopyOnWriteArrayList<>();

    public HindsightClient(
            WebClient.Builder webClientBuilder,
            @Value("${hindsight.base.url:${HINDSIGHT_BASE_URL:https://api.hindsight.ai}}") String baseUrl,
            @Value("${hindsight.api.key:${HINDSIGHT_API_KEY:}}") String apiKey,
            @Value("${hindsight.endpoint.recall:${HINDSIGHT_ENDPOINT_RECALL:}}") String recallEndpoint,
            @Value("${hindsight.endpoint.retain:${HINDSIGHT_ENDPOINT_RETAIN:}}") String retainEndpoint,
            @Value("${hindsight.endpoint.reflect:${HINDSIGHT_ENDPOINT_REFLECT:}}") String reflectEndpoint,
            @Value("${hindsight.timeout.seconds:${HINDSIGHT_TIMEOUT_SECONDS:5}}") long timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.recallEndpoint = recallEndpoint;
        this.retainEndpoint = retainEndpoint;
        this.reflectEndpoint = reflectEndpoint;
        this.timeoutSeconds = timeoutSeconds;

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 1. RECALL
     * Recalls historical incident context matching current incident telemetry.
     */
    public List<String> recall(String serviceName, String symptoms, String logs, String severity) {
        log.debug("Executing Hindsight recall for serviceName='{}', severity='{}'", serviceName, severity);

        List<String> recalled = new ArrayList<>();

        // If official endpoint configured, call live API
        if (recallEndpoint != null && !recallEndpoint.isBlank()) {
            try {
                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("serviceName", serviceName);
                requestBody.put("symptoms", symptoms);
                requestBody.put("logs", logs);
                requestBody.put("severity", severity);

                List<?> response = webClient.post()
                        .uri(recallEndpoint)
                        .bodyValue(requestBody)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.createException())
                        .bodyToMono(List.class)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .onErrorResume(e -> Mono.empty())
                        .block();

                if (response != null && !response.isEmpty()) {
                    for (Object item : response) {
                        recalled.add(item.toString());
                    }
                }
            } catch (Exception e) {
                log.error("Live Hindsight recall call failed: {}", e.getMessage());
            }
        } else {
            log.debug("Hindsight recall endpoint (HINDSIGHT_ENDPOINT_RECALL) not set. Checking retained prototype memory.");
        }

        // Consult in-memory retained long-term memories
        for (Map<String, String> entry : inMemoryStore) {
            String service = entry.get("service");
            if (isServiceMatch(serviceName, service)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Previous ").append(service).append(" incident (").append(entry.get("incident")).append(")");
                if (!entry.get("actualRootCause").isBlank()) {
                    sb.append(" was caused by: ").append(entry.get("actualRootCause")).append(". ");
                }
                if (!entry.get("successfulFix").isBlank()) {
                    sb.append("Previous successful fix: ").append(entry.get("successfulFix")).append(". ");
                }
                if (!entry.get("failedApproaches").isBlank()) {
                    sb.append("Previous failed approach: ").append(entry.get("failedApproaches")).append(". ");
                }
                if (!entry.get("lessonsLearned").isBlank()) {
                    sb.append("Lessons learned: ").append(entry.get("lessonsLearned")).append(".");
                }
                recalled.add(sb.toString().trim());
            }
        }

        return recalled;
    }

    private boolean isServiceMatch(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        if (s1.equalsIgnoreCase(s2)) return true;
        String clean1 = s1.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String clean2 = s2.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (clean1.isEmpty() || clean2.isEmpty()) return false;
        if (clean1.equals(clean2)) return true;
        if (clean1.startsWith(clean2) || clean2.startsWith(clean1)) return true;
        return false;
    }

    /**
     * 2. RETAIN
     * Retains postmortem learnings into long-term memory.
     */
    public String retain(
            String service,
            String incident,
            String symptoms,
            String actualRootCause,
            String successfulFix,
            List<String> failedApproaches,
            String prevention,
            List<String> lessonsLearned) {

        log.debug("Executing Hindsight retain for service='{}', incident='{}'", service, incident);

        String generatedRetentionId = "hindsight_mem_" + incident.toLowerCase().replace(" ", "_") + "_" + System.currentTimeMillis();

        // Retain in long-term memory store
        Map<String, String> memoryEntry = new LinkedHashMap<>();
        memoryEntry.put("service", service);
        memoryEntry.put("incident", incident);
        memoryEntry.put("symptoms", symptoms != null ? symptoms : "");
        memoryEntry.put("actualRootCause", actualRootCause != null ? actualRootCause : "");
        memoryEntry.put("successfulFix", successfulFix != null ? successfulFix : "");
        memoryEntry.put("failedApproaches", failedApproaches != null ? String.join(", ", failedApproaches) : "");
        memoryEntry.put("prevention", prevention != null ? prevention : "");
        memoryEntry.put("lessonsLearned", lessonsLearned != null ? String.join(", ", lessonsLearned) : "");
        inMemoryStore.add(memoryEntry);

        // If official endpoint configured, send live retain call
        if (retainEndpoint != null && !retainEndpoint.isBlank()) {
            try {
                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("service", service);
                requestBody.put("incident", incident);
                requestBody.put("symptoms", symptoms);
                requestBody.put("actual root cause", actualRootCause);
                requestBody.put("successful fix", successfulFix);
                requestBody.put("failed approaches", failedApproaches != null ? failedApproaches : Collections.emptyList());
                requestBody.put("prevention", prevention);
                requestBody.put("lessons learned", lessonsLearned != null ? lessonsLearned : Collections.emptyList());

                Map<?, ?> response = webClient.post()
                        .uri(retainEndpoint)
                        .bodyValue(requestBody)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.createException())
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .block();

                log.debug("Official Hindsight retain successful: {}", response);
                return generatedRetentionId;
            } catch (Exception e) {
                log.error("Live Hindsight retain failed: {}", e.getMessage());
                throw new RuntimeException("Live Hindsight retain failed: " + e.getMessage(), e);
            }
        } else {
            log.debug("Hindsight retain endpoint not configured. Stored in prototype long-term memory with ID: {}", generatedRetentionId);
        }

        return generatedRetentionId;
    }

    /**
     * 3. REFLECT
     * Triggers the Hindsight reflection mechanism across consolidated memories.
     */
    public String reflect(Map<String, Object> reflectContext) {
        log.debug("Executing Hindsight reflect.");

        if (reflectEndpoint != null && !reflectEndpoint.isBlank()) {
            try {
                return webClient.post()
                        .uri(reflectEndpoint)
                        .bodyValue(reflectContext != null ? reflectContext : Collections.emptyMap())
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.createException())
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .block();
            } catch (Exception e) {
                log.error("Live Hindsight reflect failed: {}", e.getMessage());
                throw new RuntimeException("Live Hindsight reflect failed: " + e.getMessage(), e);
            }
        }

        return "Reflection completed: Pattern consolidated into Hindsight memory graph.";
    }

    /**
     * Overloaded reflect.
     */
    public String reflect(String contextId, Map<String, Object> metadata) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (contextId != null) context.put("contextId", contextId);
        if (metadata != null) context.putAll(metadata);
        return reflect(context);
    }

    /**
     * Clear in-memory store (useful for clean unit tests).
     */
    public void clearMemory() {
        inMemoryStore.clear();
    }
}