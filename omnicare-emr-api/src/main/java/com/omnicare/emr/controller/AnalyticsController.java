package com.omnicare.emr.controller;
import com.omnicare.emr.dto.OperationalKpiResponseDto;
import com.omnicare.emr.dto.PatientClinicalHistoryResponseDto;
import com.omnicare.emr.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Analytics and Clinical Intelligence", description = "Operational KPI analytics and consolidated patient medical history endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/analytics/operational-kpis")
    @Operation(summary = "Retrieve Operational KPIs", description = "Calculates operational clinical KPIs including lab TAT, encounter status breakdown, and top ICD-10 diagnoses")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operational KPIs retrieved successfully")
    })
    public ResponseEntity<OperationalKpiResponseDto> getOperationalKpis() {
        return ResponseEntity.ok(analyticsService.getOperationalKpis());
    }

    @GetMapping("/patients/{id}/clinical-history")
    @Operation(summary = "Get Patient Clinical History Timeline", description = "Consolidates all encounters,, vitals, lab reports, diagnoses, and prescriptions into a unified chronological medical timeline for a patient")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient clinical history timeline retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    public ResponseEntity<PatientClinicalHistoryResponseDto> getPatientClinicalHistory(
            @Parameter(description = "UUID of the patient", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(analyticsService.getPatientClinicalHistory(id));
    }
}