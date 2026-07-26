package com.omnicare.emr.service;

import com.omnicare.emr.dto.EncounterRequestDto;
import com.omnicare.emr.dto.EncounterResponseDto;
import com.omnicare.emr.dto.FinalizeEncounterRequestDto;
import com.omnicare.emr.dto.FinalizeEncounterResponseDto;
import com.omnicare.emr.entity.EncounterStatus;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining business operations for Encounter management.
 */
public interface EncounterService {

    /**
     * Creates a new encounter. Defaults status to PLANNED if not specified.
     *
     * @param requestDto creation details
     * @return created encounter response DTO
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if Patient or Practitioner does not exist
     */
    EncounterResponseDto createEncounter(EncounterRequestDto requestDto);

    /**
     * Retrieves an encounter by ID.
     *
     * @param id UUID of the encounter
     * @return encounter response DTO
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if encounter is not found or soft-deleted
     */
    EncounterResponseDto getEncounterById(UUID id);

    /**
     * Retrieves all active (non-soft-deleted) encounters.
     *
     * @return list of encounter response DTOs
     */
    List<EncounterResponseDto> getAllEncounters();

    /**
     * Updates an encounter's status.
     */
    EncounterResponseDto updateEncounterStatus(UUID id, EncounterStatus status);

    /**
     * Finalizes an encounter by persisting diagnoses and prescription items, and transitioning status to FINISHED.
     */
    FinalizeEncounterResponseDto finalizeEncounter(UUID id, FinalizeEncounterRequestDto requestDto);
}
