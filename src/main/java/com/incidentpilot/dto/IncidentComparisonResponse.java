package com.incidentpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing the "Without Memory" vs "With Memory" comparison analysis.
 * 
 * Demonstrates the core demo differentiator:
 * - Running the same incident twice:
 *   1. Baseline diagnosis WITHOUT memory (generic SRE cold start)
 *   2. Memory-augmented diagnosis WITH recalled Hindsight memory
 * - Highlights the concrete advantages memory provides (root cause precision, avoided traps, proven fixes).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentComparisonResponse {
    private String serviceName;
    private String severity;
    private String symptoms;
    private String logs;
    
    @Builder.Default
    private List<String> recalledMemories = new ArrayList<>();
    
    private IncidentAnalysisResponse withoutMemoryDiagnosis;
    private IncidentAnalysisResponse withMemoryDiagnosis;
    
    @Builder.Default
    private List<String> comparisonHighlights = new ArrayList<>();
    
    private boolean memoryAdvantageDemonstrated;
    private String summary;
}
