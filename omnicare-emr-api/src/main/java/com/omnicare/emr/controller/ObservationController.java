package com.omnicare.emr.controller;

import com.omnicare.emr.dto.ObservationRequestDto;
import com.omnicare.emr.dto.ObservationResponseDto;
import com.omnicare.emr.service.ObservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing clinical observations and vitals.
 */
@RestController
@Tag(name = "Observations", description = "Clinical Observation and Vitals Management APIs")
@RequestMapping("/api/v1/observations")
@RequiredArgsConstructor
public class ObservationController {

    private final ObservationService observationService;

    /**
     * Endpoint to record a new clinical observation / vitals record.
     *
     * @param request JSON payload containing encounter ID and observation JSON payload
     * @return 201 Created status with saved observation details
     */
    @Operation(summary = "Record clinical observation / vitals", description = "Records a new observation or vitals JSON payload linked to an active encounter. Fails if the encounter is CANCELLED or not found.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Observation recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or encounter is CANCELLED"),
            @ApiResponse(responseCode = "404", description = "Referenced encounter not found"),
            @ApiResponse(responseCode = "409", description = "Conflict due to cancelled encounter status")
    })
    @PostMapping
    public ResponseEntity<ObservationResponseDto> createObservation(@Valid @RequestBody ObservationRequestDto request) {
        ObservationResponseDto response = observationService.createObservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to retrieve observations filtered by encounter ID.
     *
     * @param encounterId UUID of the encounter
     * @return 200 OK status with list of observations for the encounter
     */
    @Operation(summary = "Get observations by encounter ID", description = "Retrieves all clinical observations recorded for a specific encounter ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Observations retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Referenced encounter not found")
    })
    @GetMapping
    public ResponseEntity<List<ObservationResponseDto>> getObservationsByEncounterId(
            @Parameter(description = "UUID of the encounter to filter observations", required = true)
            @RequestParam("encounterId") UUID encounterId) {
        List<ObservationResponseDto> response = observationService.getObservationsByEncounterId(encounterId);
        return ResponseEntity.ok(response);
    }
}
