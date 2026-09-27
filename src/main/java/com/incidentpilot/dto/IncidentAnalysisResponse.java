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
public class IncidentAnalysisResponse {
    private String serviceName;
    private String severity;
    private List<String> possibleRootCauses;
    private List<String> investigationSteps;
    private List<String> recommendedFixes;
    private List<String> historicalIncidents;
    private List<String> memoryBasedInsights;
}