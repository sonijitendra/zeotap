package com.ims.controller;

import com.ims.dto.request.SignalRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.service.SignalIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/signals")
@Tag(name = "Signal Ingestion", description = "Endpoints for ingesting monitoring signals")
public class SignalController {

    private static final Logger log = LoggerFactory.getLogger(SignalController.class);
    private final SignalIngestionService ingestionService;

    public SignalController(SignalIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    @Operation(summary = "Ingest a single signal")
    public ResponseEntity<ApiResponse<String>> ingestSignal(@Valid @RequestBody SignalRequest signal) {
        ingestionService.ingest(signal);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Signal accepted for processing", "ACCEPTED"));
    }

    @PostMapping("/batch")
    @Operation(summary = "Ingest signals in batch")
    public ResponseEntity<ApiResponse<String>> ingestBatch(@Valid @RequestBody List<SignalRequest> signals) {
        ingestionService.ingestBatch(signals);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(signals.size() + " signals accepted", "ACCEPTED"));
    }
}
