package com.incidentpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentRequest {
    private String incidentId;
    private String title;
    private String description;
    private String service;
    private String severity;
    private String logs;
    private Map<String, Object> metrics;
    private String timestamp;
}
