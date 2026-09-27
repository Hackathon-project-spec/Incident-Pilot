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
public class IncidentResponse {
    private String incidentId;
    private String diagnosis;
    private String suspectedRootCause;
    private List<String> suggestedMitigations;
    private List<String> recalledMemories;
    private String status;
}
