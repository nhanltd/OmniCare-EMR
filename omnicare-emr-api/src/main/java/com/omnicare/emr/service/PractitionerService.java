package com.omnicare.emr.service;

import com.omnicare.emr.dto.PractitionerRequestDto;
import com.omnicare.emr.dto.PractitionerResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining business operations for Practitioner management.
 */
public interface PractitionerService {

    /**
     * Creates a new practitioner record.
     *
     * @param requestDto creation details
     * @return created practitioner response details
     * @throws com.omnicare.emr.exception.DuplicateResourceException if practitionerCode already exists
     */
    PractitionerResponseDto createPractitioner(PractitionerRequestDto requestDto);

    /**
     * Retrieves all active (non-soft-deleted) practitioners.
     *
     * @return list of active practitioner response DTOs
     */
    List<PractitionerResponseDto> getAllPractitioners();

    /**
     * Retrieves a practitioner by ID.
     *
     * @param id UUID of the practitioner
     * @return practitioner response details
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if practitioner does not exist or is soft-deleted
     */
    PractitionerResponseDto getPractitionerById(UUID id);

    /**
     * Updates an existing practitioner record.
     *
     * @param id UUID of practitioner to update
     * @param requestDto updated practitioner details
     * @return updated practitioner response details
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if practitioner does not exist or is soft-deleted
     * @throws com.omnicare.emr.exception.DuplicateResourceException if practitionerCode is used by another practitioner
     */
    PractitionerResponseDto updatePractitioner(UUID id, PractitionerRequestDto requestDto);

    /**
     * Performs soft deletion of a practitioner by setting isDeleted = true.
     *
     * @param id UUID of practitioner to delete
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if practitioner does not exist or is already soft-deleted
     */
    void deletePractitioner(UUID id);
}
