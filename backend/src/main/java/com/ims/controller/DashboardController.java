package com.ims.controller;

import com.ims.dto.response.ApiResponse;
import com.ims.dto.response.DashboardResponse;
import com.ims.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Aggregated dashboard metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Get dashboard summary metrics")
    public ResponseEntity<ApiResponse<DashboardResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardSummary()));
    }
}
