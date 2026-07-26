package com.omnicare.emr.controller;

import com.omnicare.emr.dto.EncounterRequestDto;
import com.omnicare.emr.dto.EncounterResponseDto;
import com.omnicare.emr.dto.FinalizeEncounterRequestDto;
import com.omnicare.emr.dto.FinalizeEncounterResponseDto;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.service.EncounterService;
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
 * REST controller for managing clinical encounters.
 */
@RestController
@Tag(name = "Encounters", description = "Clinical Encounter Management APIs")
@RequestMapping("/api/v1/encounters")
@RequiredArgsConstructor
public class EncounterController {

    private final EncounterService encounterService;

    /**
     * Endpoint to create a new planned clinical encounter.
     *
     * @param request JSON payload containing encounter details
     * @return 201 Created status with created encounter details
     */
    @Operation(summary = "Create a new clinical encounter", description = "Registers a new clinical encounter for a patient and practitioner. Sets status to PLANNED by default if omitted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Encounter created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or validation failure"),
            @ApiResponse(responseCode = "404", description = "Referenced Patient or Practitioner not found")
    })
    @PostMapping
    public ResponseEntity<EncounterResponseDto> createEncounter(@Valid @RequestBody EncounterRequestDto request) {
        EncounterResponseDto response = encounterService.createEncounter(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to retrieve all clinical encounters.
     *
     * @return 200 OK status with list of active encounters
     */
    @Operation(summary = "Get all encounters", description = "Retrieves a list of all active clinical encounters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Encounters retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<EncounterResponseDto>> getAllEncounters() {
        List<EncounterResponseDto> response = encounterService.getAllEncounters();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to fetch a specific encounter by UUID.
     *
     * @param id UUID of the encounter
     * @return 200 OK status with encounter details
     */
    @Operation(summary = "Get encounter by ID", description = "Fetches details of a specific clinical encounter by its UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Encounter retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Encounter not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EncounterResponseDto> getEncounterById(
            @Parameter(description = "UUID of the encounter", required = true)
            @PathVariable("id") UUID id) {
        EncounterResponseDto response = encounterService.getEncounterById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to update an encounter's status.
     *
     * @param id UUID of the encounter
     * @param status new status
     * @return 200 OK with updated encounter details
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<EncounterResponseDto> updateEncounterStatus(
            @PathVariable("id") UUID id,
            @RequestParam("status") EncounterStatus status) {
        EncounterResponseDto response = encounterService.updateEncounterStatus(id, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to finalize a clinical encounter.
     *
     * @param id UUID of the encounter
     * @param request payload containing diagnoses and prescriptions
     * @return 200 OK status with final encounter response
     */
    @Operation(summary = "Finalize encounter", description = "Finalizes a clinical encounter with diagnoses and prescriptions. Sets status to FINISHED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Encounter finalized successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error, encounter cancelled, or invalid prescription"),
            @ApiResponse(responseCode = "404", description = "Encounter not found")
    })
    @PostMapping("/{id}/finalize")
    public ResponseEntity<FinalizeEncounterResponseDto> finalizeEncounter(
            @PathVariable("id") UUID id,
            @Valid @RequestBody FinalizeEncounterRequestDto request) {
        FinalizeEncounterResponseDto response = encounterService.finalizeEncounter(id, request);
        return ResponseEntity.ok(response);
    }
}
