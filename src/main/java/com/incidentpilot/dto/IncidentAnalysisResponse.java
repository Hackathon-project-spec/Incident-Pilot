package com.incidentpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured incident diagnosis and analysis output.
 * 
 * Contains:
 * - root cause hypothesis (possibleRootCauses)
 * - investigation steps (investigationSteps)
 * - possible / recommended fixes (recommendedFixes)
 * - related past incidents (historicalIncidents)
 * - memory-based insights & failed approaches to avoid
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentAnalysisResponse {
    private String serviceName;
    private String severity;
    
    @Builder.Default
    private List<String> possibleRootCauses = new ArrayList<>();
    
    @Builder.Default
    private List<String> investigationSteps = new ArrayList<>();
    
    @Builder.Default
    private List<String> recommendedFixes = new ArrayList<>();
    
    @Builder.Default
    private List<String> historicalIncidents = new ArrayList<>();
    
    @Builder.Default
    private List<String> memoryBasedInsights = new ArrayList<>();
    
    @Builder.Default
    private List<String> failedApproachesToAvoid = new ArrayList<>();
    
    private String confidenceScore;

    // Convenience aliases for flexible frontend / consumer naming conventions
    public List<String> getRootCauseHypothesis() {
        return possibleRootCauses;
    }

    public List<String> getPossibleFixes() {
        return recommendedFixes;
    }

    public List<String> getRelatedPastIncidents() {
        return historicalIncidents;
    }
}