package com.omnicare.emr.controller;

import com.omnicare.emr.dto.PractitionerRequestDto;
import com.omnicare.emr.dto.PractitionerResponseDto;
import com.omnicare.emr.service.PractitionerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing healthcare practitioner resources.
 */
@RestController
@Tag(name = "Practitioner Management", description = "APIs for managing healthcare practitioners (Doctors, Nurses, Technicians)")
@RequestMapping("/api/v1/practitioners")
@RequiredArgsConstructor
public class PractitionerController {

    private final PractitionerService practitionerService;

    /**
     * Endpoint to create a new practitioner.
     *
     * @param request JSON payload containing practitioner details
     * @return 201 Created status with saved practitioner details
     */
    @Operation(summary = "Create a new practitioner", description = "Registers a new healthcare practitioner with their professional and contact details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Practitioner created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body payload or validation failure"),
            @ApiResponse(responseCode = "409", description = "Practitioner with the given code already exists")
    })
    @PostMapping
    public ResponseEntity<PractitionerResponseDto> createPractitioner(@Valid @RequestBody PractitionerRequestDto request) {
        PractitionerResponseDto response = practitionerService.createPractitioner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to fetch all active (non-soft-deleted) practitioners.
     *
     * @return 200 OK status with a list of active practitioners
     */
    @Operation(summary = "Get all active practitioners", description = "Retrieves a list of all active (non-soft-deleted) healthcare practitioners.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Practitioners retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<PractitionerResponseDto>> getAllPractitioners() {
        List<PractitionerResponseDto> response = practitionerService.getAllPractitioners();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve a practitioner by their UUID.
     *
     * @param id UUID of practitioner to fetch
     * @return 200 OK status with practitioner details
     */
    @Operation(summary = "Get practitioner by ID", description = "Fetches details of a specific active practitioner by their UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Practitioner retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Practitioner not found or soft-deleted")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PractitionerResponseDto> getPractitionerById(
            @Parameter(description = "UUID of the practitioner", required = true)
            @PathVariable("id") UUID id) {
        PractitionerResponseDto response = practitionerService.getPractitionerById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to update an existing practitioner record.
     *
     * @param id UUID of practitioner to update
     * @param request updated practitioner payload
     * @return 200 OK status with updated practitioner details
     */
    @Operation(summary = "Update practitioner details", description = "Updates professional and contact details of an existing active practitioner.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Practitioner updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body payload or validation failure"),
            @ApiResponse(responseCode = "404", description = "Practitioner not found or soft-deleted"),
            @ApiResponse(responseCode = "409", description = "Practitioner code is already used by another practitioner")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PractitionerResponseDto> updatePractitioner(
            @Parameter(description = "UUID of the practitioner to update", required = true)
            @PathVariable("id") UUID id,
            @Valid @RequestBody PractitionerRequestDto request) {
        PractitionerResponseDto response = practitionerService.updatePractitioner(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to soft-delete a practitioner record.
     *
     * @param id UUID of practitioner to soft-delete
     * @return 204 No Content status
     */
    @Operation(summary = "Soft delete practitioner", description = "Soft deletes a practitioner by setting their deletion flag to true.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Practitioner soft-deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Practitioner not found or already soft-deleted")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deletePractitioner(
            @Parameter(description = "UUID of the practitioner to delete", required = true)
            @PathVariable("id") UUID id) {
        practitionerService.deletePractitioner(id);
        return ResponseEntity.noContent().build();
    }
}
