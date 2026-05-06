package com.ims.controller;

import com.ims.dto.request.RcaRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.dto.response.RcaResponse;
import com.ims.service.RcaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/rca")
@Tag(name = "Root Cause Analysis", description = "RCA submission and retrieval")
public class RcaController {

    private final RcaService rcaService;

    public RcaController(RcaService rcaService) {
        this.rcaService = rcaService;
    }

    @PostMapping
    @Operation(summary = "Submit RCA for an incident")
    public ResponseEntity<ApiResponse<RcaResponse>> submit(
            @PathVariable UUID incidentId, @Valid @RequestBody RcaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(rcaService.submitRca(incidentId, request)));
    }

    @GetMapping
    @Operation(summary = "Get RCA for an incident")
    public ResponseEntity<ApiResponse<RcaResponse>> get(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(ApiResponse.success(rcaService.getRca(incidentId)));
    }
}
