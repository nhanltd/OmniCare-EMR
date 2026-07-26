package com.omnicare.emr.controller;

import com.omnicare.emr.dto.DiagnosticReportCreateRequestDto;
import com.omnicare.emr.dto.DiagnosticReportResponseDto;
import com.omnicare.emr.dto.DiagnosticReportResultUpdateDto;
import com.omnicare.emr.service.DiagnosticReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing diagnostic reports and handling LIS webhooks.
 */
@RestController
@Tag(name = "Diagnostic Reports", description = "Diagnostic Report Management & LIS Webhook APIs")
@RequestMapping("/api/v1/diagnostic-reports")
@RequiredArgsConstructor
public class DiagnosticReportController {

    private final DiagnosticReportService diagnosticReportService;

    /**
     * Endpoint to create a new diagnostic report order.
     *
     * @param requestDto JSON payload containing diagnostic report creation details
     * @return 201 Created status with created diagnostic report details
     */
    @Operation(summary = "Create a diagnostic report order", description = "Registers a new lab/diagnostic test order for an active clinical encounter.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Diagnostic report created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DiagnosticReportResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or encounter cancelled"),
            @ApiResponse(responseCode = "404", description = "Referenced encounter not found")
    })
    @PostMapping
    public ResponseEntity<DiagnosticReportResponseDto> createDiagnosticReport(
            @Parameter(description = "Diagnostic report order payload", required = true)
            @Valid @RequestBody DiagnosticReportCreateRequestDto requestDto) {
        DiagnosticReportResponseDto response = diagnosticReportService.createDiagnosticReport(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint for LIS webhook to update diagnostic report test results.
     *
     * @param id UUID of the diagnostic report
     * @param resultDto JSON payload containing test results
     * @return 200 OK status with updated diagnostic report details
     */
    @Operation(summary = "Update diagnostic report results (LIS Webhook)", description = "Updates lab results and status for a diagnostic report order. Used by LIS integration.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Diagnostic report results updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DiagnosticReportResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or associated encounter cancelled"),
            @ApiResponse(responseCode = "404", description = "Diagnostic report not found")
    })
    @PutMapping("/{id}/results")
    public ResponseEntity<DiagnosticReportResponseDto> updateDiagnosticReportResults(
            @Parameter(description = "UUID of the diagnostic report", required = true)
            @PathVariable("id") UUID id,
            @Parameter(description = "Diagnostic report result update payload", required = true)
            @Valid @RequestBody DiagnosticReportResultUpdateDto resultDto) {
        DiagnosticReportResponseDto response = diagnosticReportService.updateDiagnosticReportResults(id, resultDto);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to fetch a specific diagnostic report by UUID.
     *
     * @param id UUID of the diagnostic report
     * @return 200 OK status with diagnostic report details
     */
    @Operation(summary = "Get diagnostic report by ID", description = "Fetches details of a specific diagnostic report by its UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Diagnostic report retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DiagnosticReportResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Diagnostic report not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DiagnosticReportResponseDto> getDiagnosticReportById(
            @Parameter(description = "UUID of the diagnostic report", required = true)
            @PathVariable("id") UUID id) {
        DiagnosticReportResponseDto response = diagnosticReportService.getDiagnosticReportById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve diagnostic reports filtered by encounter ID.
     *
     * @param encounterId UUID of the encounter
     * @return 200 OK status with list of diagnostic reports
     */
    @Operation(summary = "Get diagnostic reports by encounter ID", description = "Retrieves all diagnostic report orders recorded for a specific clinical encounter.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Diagnostic reports retrieved successfully",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DiagnosticReportResponseDto.class)))),
            @ApiResponse(responseCode = "404", description = "Referenced encounter not found")
    })
    @GetMapping
    public ResponseEntity<List<DiagnosticReportResponseDto>> getDiagnosticReportsByEncounterId(
            @Parameter(description = "UUID of the encounter to filter diagnostic reports", required = true)
            @RequestParam("encounterId") UUID encounterId) {
        List<DiagnosticReportResponseDto> response = diagnosticReportService.getDiagnosticReportsByEncounterId(encounterId);
        return ResponseEntity.ok(response);
    }
}
