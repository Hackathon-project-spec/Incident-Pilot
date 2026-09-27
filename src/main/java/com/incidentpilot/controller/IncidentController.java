package com.incidentpilot.controller;

import com.incidentpilot.dto.IncidentAnalysisRequest;
import com.incidentpilot.dto.IncidentAnalysisResponse;
import com.incidentpilot.dto.IncidentRequest;
import com.incidentpilot.dto.IncidentResponse;
import com.incidentpilot.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Incident Operations
 */
@Slf4j
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    /**
     * POST /api/incidents/analyze
     * Analyzes an incident using Hindsight memory recall and LLM reasoning.
     */
    @PostMapping("/analyze")
    public ResponseEntity<IncidentAnalysisResponse> analyze(@RequestBody(required = false) IncidentAnalysisRequest request) {
        return ResponseEntity.ok(incidentService.analyze(request));
    }

    /**
     * POST /api/incidents/diagnose
     */
    @PostMapping("/diagnose")
    public ResponseEntity<IncidentResponse> diagnose(@RequestBody IncidentRequest request) {
        log.info("Received incident diagnosis request for incidentId: {}", request.getIncidentId());
        return ResponseEntity.ok(incidentService.processIncident(request));
    }
}