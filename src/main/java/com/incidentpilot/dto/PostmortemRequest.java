package com.incidentpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostmortemRequest {
    private String incidentId;
    private String serviceName;
    private String actualRootCause;
    private String successfulFix;
    private Object failedApproaches;
    private String preventionStrategy;
    private Object lessonsLearned;

    // Aliases / legacy compatibility fields
    private String service;
    private String incident;
    private String symptoms;
    private String prevention;
    private String title;
    private String rootCause;
    private String timeline;
    private String resolution;
    private List<String> actionItems;

    public String getEffectiveService() {
        if (serviceName != null && !serviceName.isBlank()) return serviceName;
        if (service != null && !service.isBlank()) return service;
        return "unknown";
    }

    public String getEffectiveIncidentId() {
        if (incidentId != null && !incidentId.isBlank()) return incidentId;
        if (incident != null && !incident.isBlank()) return incident;
        return "INC-UNKNOWN";
    }

    public String getEffectiveActualRootCause() {
        if (actualRootCause != null && !actualRootCause.isBlank()) return actualRootCause;
        if (rootCause != null && !rootCause.isBlank()) return rootCause;
        return "Unspecified root cause";
    }

    public String getEffectiveSuccessfulFix() {
        if (successfulFix != null && !successfulFix.isBlank()) return successfulFix;
        if (resolution != null && !resolution.isBlank()) return resolution;
        return "Unspecified fix";
    }

    public String getEffectiveFailedApproaches() {
        if (failedApproaches == null) return "";
        if (failedApproaches instanceof List<?> list) {
            return String.join(", ", list.stream().map(Object::toString).toList());
        }
        return failedApproaches.toString();
    }

    public String getEffectivePreventionStrategy() {
        if (preventionStrategy != null && !preventionStrategy.isBlank()) return preventionStrategy;
        if (prevention != null && !prevention.isBlank()) return prevention;
        return "";
    }

    public String getEffectiveLessonsLearned() {
        if (lessonsLearned == null) return "";
        if (lessonsLearned instanceof List<?> list) {
            return String.join(", ", list.stream().map(Object::toString).toList());
        }
        return lessonsLearned.toString();
    }
}