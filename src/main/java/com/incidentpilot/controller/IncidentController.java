package com.incidentpilot.controller;

import com.incidentpilot.dto.IncidentAnalysisRequest;
import com.incidentpilot.dto.IncidentAnalysisResponse;
import com.incidentpilot.dto.IncidentComparisonResponse;
import com.incidentpilot.dto.IncidentRequest;
import com.incidentpilot.dto.IncidentResponse;
import com.incidentpilot.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Incident Operations and Comparison.
 */
@Slf4j
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
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
     * POST /api/incidents/compare
     * Runs the incident twice (without memory vs with memory) and returns a side-by-side comparison.
     */
    @PostMapping("/compare")
    public ResponseEntity<IncidentComparisonResponse> compare(@RequestBody(required = false) IncidentAnalysisRequest request) {
        log.info("Received incident memory comparison request");
        return ResponseEntity.ok(incidentService.compareWithAndWithoutMemory(request));
    }

    /**
     * GET /api/incidents/compare
     * Quick browser access to memory comparison for the default incident.
     */
    @GetMapping("/compare")
    public ResponseEntity<IncidentComparisonResponse> getComparison() {
        return ResponseEntity.ok(incidentService.compareWithAndWithoutMemory(null));
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