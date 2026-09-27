package com.incidentpilot.controller;

import com.incidentpilot.dto.PostmortemRequest;
import com.incidentpilot.dto.PostmortemResponse;
import com.incidentpilot.service.PostmortemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Postmortem Operations (Workflow 2: Retain & Reflect)
 */
@Slf4j
@RestController
@RequestMapping("/api/postmortems")
@RequiredArgsConstructor
public class PostmortemController {

    private final PostmortemService postmortemService;

    @PostMapping
    public ResponseEntity<PostmortemResponse> submitPostmortem(@RequestBody(required = false) PostmortemRequest request) {
        PostmortemResponse response = postmortemService.processPostmortem(request);
        return ResponseEntity.ok(response);
    }
}
