package com.ims.controller;

import com.ims.dto.request.StateTransitionRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.dto.response.IncidentResponse;
import com.ims.dto.response.SignalResponse;
import com.ims.dto.response.TimelineResponse;
import com.ims.model.mongo.RawSignal;
import com.ims.repository.mongo.RawSignalRepository;
import com.ims.service.IncidentService;
import com.ims.service.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incident Management", description = "Incident lifecycle and query endpoints")
public class IncidentController {

    private final IncidentService incidentService;
    private final SseEmitterService sseEmitterService;
    private final RawSignalRepository rawSignalRepository;

    public IncidentController(IncidentService incidentService, SseEmitterService sseEmitterService,
                              RawSignalRepository rawSignalRepository) {
        this.incidentService = incidentService;
        this.sseEmitterService = sseEmitterService;
        this.rawSignalRepository = rawSignalRepository;
    }

    @GetMapping
    @Operation(summary = "List incidents with optional filters")
    public ResponseEntity<ApiResponse<Page<IncidentResponse>>> list(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String componentId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                incidentService.listIncidents(state, severity, componentId, pageable)));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active incidents sorted by severity")
    public ResponseEntity<ApiResponse<List<IncidentResponse>>> active() {
        return ResponseEntity.ok(ApiResponse.success(incidentService.getActiveIncidents()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident details")
    public ResponseEntity<ApiResponse<IncidentResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(incidentService.getIncident(id)));
    }

    @PatchMapping("/{id}/transition")
    @Operation(summary = "Transition incident state")
    public ResponseEntity<ApiResponse<IncidentResponse>> transition(
            @PathVariable UUID id, @Valid @RequestBody StateTransitionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(incidentService.transitionState(id, request)));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Get incident timeline")
    public ResponseEntity<ApiResponse<List<TimelineResponse>>> timeline(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(incidentService.getTimeline(id)));
    }

    @GetMapping("/{id}/signals")
    @Operation(summary = "Get signals linked to an incident")
    public ResponseEntity<ApiResponse<List<SignalResponse>>> signals(@PathVariable UUID id) {
        List<SignalResponse> sigs = rawSignalRepository.findByIncidentIdOrderByTimestampDesc(id.toString())
                .stream().map(r -> SignalResponse.builder().signalId(r.getSignalId())
                        .componentId(r.getComponentId()).severity(r.getSeverity())
                        .timestamp(r.getTimestamp()).message(r.getMessage())
                        .metadata(r.getMetadata()).incidentId(r.getIncidentId())
                        .processed(r.isProcessed()).processedAt(r.getProcessedAt()).build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(sigs));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream for real-time incident updates")
    public SseEmitter stream() {
        return sseEmitterService.createEmitter();
    }
}
