package com.incidentpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostmortemResponse {
    private String incidentId;
    private String serviceName;
    private String retentionStatus;
    private String retentionId;
    private String reflectionStatus;
    private String reflectionSummary;
    private String status;
    private String message;
}