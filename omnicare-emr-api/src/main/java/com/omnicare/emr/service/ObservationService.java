package com.omnicare.emr.service;

import com.omnicare.emr.dto.ObservationRequestDto;
import com.omnicare.emr.dto.ObservationResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining business operations for Observation recording.
 */
public interface ObservationService {

    /**
     * Records a new clinical observation for an encounter.
     *
     * @param requestDto observation payload details
     * @return recorded observation response DTO
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if Encounter is not found (HTTP 404)
     * @throws com.omnicare.emr.exception.EncounterCancelledException if Encounter status is CANCELLED (HTTP 400/409)
     */
    ObservationResponseDto createObservation(ObservationRequestDto requestDto);

    /**
     * Retrieves all observations associated with an encounter.
     *
     * @param encounterId UUID of the encounter
     * @return list of observation response DTOs
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if Encounter is not found
     */
    List<ObservationResponseDto> getObservationsByEncounterId(UUID encounterId);
}
