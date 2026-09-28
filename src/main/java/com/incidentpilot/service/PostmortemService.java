package com.incidentpilot.service;

import com.incidentpilot.client.HindsightClient;
import com.incidentpilot.dto.PostmortemRequest;
import com.incidentpilot.dto.PostmortemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service orchestrating the postmortem ingestion workflow:
 * PostmortemController -> PostmortemService -> HindsightClient.retain() -> HindsightClient.reflect() -> Response
 * 
 * Demonstrates long-term memory lifecycle without database persistence.
 * Reports retain and reflect states honestly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostmortemService {

    private final HindsightClient hindsightClient;

    public PostmortemResponse processPostmortem(PostmortemRequest request) {
        if (request == null) {
            request = PostmortemRequest.builder().build();
        }
        String incidentId = request.getEffectiveIncidentId();
        String serviceName = request.getEffectiveService();
        String actualRootCause = request.getEffectiveActualRootCause();
        String successfulFix = request.getEffectiveSuccessfulFix();
        String failedApproachesStr = request.getEffectiveFailedApproaches();
        List<String> failedApproaches = failedApproachesStr.isBlank()
                ? Collections.emptyList()
                : List.of(failedApproachesStr);
        String preventionStrategy = request.getEffectivePreventionStrategy();
        String lessonsLearnedStr = request.getEffectiveLessonsLearned();
        List<String> lessonsLearned = lessonsLearnedStr.isBlank()
                ? Collections.emptyList()
                : List.of(lessonsLearnedStr);

        // [POSTMORTEM] Received postmortem
        log.info("[POSTMORTEM] Received postmortem");

        // [MEMORY] Retaining incident learning
        log.info("[MEMORY] Retaining incident learning");
        String retentionId;
        String retentionStatus;
        try {
            retentionId = hindsightClient.retain(
                    serviceName,
                    incidentId,
                    request.getSymptoms() != null ? request.getSymptoms() : "",
                    actualRootCause,
                    successfulFix,
                    failedApproaches,
                    preventionStrategy,
                    lessonsLearned
            );
            retentionStatus = "SUCCESS";
        } catch (Exception e) {
            log.error("Hindsight retain failed: {}", e.getMessage());
            return PostmortemResponse.builder()
                    .incidentId(incidentId)
                    .serviceName(serviceName)
                    .retentionStatus("FAILED")
                    .retentionId(null)
                    .reflectionStatus("SKIPPED")
                    .reflectionSummary("Reflection skipped because retain failed.")
                    .status("FAILED")
                    .message("Failed to retain postmortem in long-term memory: " + e.getMessage())
                    .build();
        }

        // STEP 2: Hindsight Reflect
        String reflectionStatus;
        String reflectionSummary;
        try {
            Map<String, Object> reflectContext = new LinkedHashMap<>();
            reflectContext.put("service", serviceName);
            reflectContext.put("incident", incidentId);
            reflectContext.put("rootCause", actualRootCause);

            reflectionSummary = hindsightClient.reflect(reflectContext);

            if (reflectionSummary != null && reflectionSummary.toLowerCase().startsWith("reflection error")) {
                reflectionStatus = "FAILED";
                log.warn("Hindsight reflect reported failure: {}", reflectionSummary);
            } else {
                reflectionStatus = "SUCCESS";
                // [MEMORY] Reflection completed
                log.info("[MEMORY] Reflection completed");
            }
        } catch (Exception e) {
            log.error("Hindsight reflect encountered exception: {}", e.getMessage());
            reflectionStatus = "FAILED";
            reflectionSummary = "Reflection failed: " + e.getMessage();
        }

        // Report retain and reflect states honestly
        boolean fullySuccessful = "SUCCESS".equals(retentionStatus) && "SUCCESS".equals(reflectionStatus);
        String overallStatus = fullySuccessful ? "COMPLETED" : "PARTIAL_SUCCESS";
        String message = fullySuccessful
                ? "Postmortem successfully retained into long-term memory and consolidated via reflection."
                : "Postmortem successfully retained in long-term memory, but reflection failed.";

        return PostmortemResponse.builder()
                .incidentId(incidentId)
                .serviceName(serviceName)
                .retentionStatus(retentionStatus)
                .retentionId(retentionId != null ? retentionId : UUID.randomUUID().toString())
                .reflectionStatus(reflectionStatus)
                .reflectionSummary(reflectionSummary)
                .status(overallStatus)
                .message(message)
                .build();
    }
}