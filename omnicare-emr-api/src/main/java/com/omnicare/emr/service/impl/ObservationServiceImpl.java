package com.omnicare.emr.service.impl;

import com.omnicare.emr.dto.ObservationRequestDto;
import com.omnicare.emr.dto.ObservationResponseDto;
import com.omnicare.emr.dto.mapper.ObservationMapper;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Observation;
import com.omnicare.emr.exception.EncounterCancelledException;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.repository.EncounterRepository;
import com.omnicare.emr.repository.ObservationRepository;
import com.omnicare.emr.service.ObservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link ObservationService}.
 */
@Service
@RequiredArgsConstructor
public class ObservationServiceImpl implements ObservationService {

    private final ObservationRepository observationRepository;
    private final EncounterRepository encounterRepository;
    private final ObservationMapper observationMapper;

    @Override
    @Transactional
    public ObservationResponseDto createObservation(ObservationRequestDto requestDto) {
        Encounter encounter = encounterRepository.findByIdAndIsDeletedFalse(requestDto.getEncounterId())
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + requestDto.getEncounterId()));

        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new EncounterCancelledException("Cannot record observation for cancelled encounter with ID: " + encounter.getId());
        }

        Observation observation = observationMapper.toEntity(requestDto);
        observation.setEncounter(encounter);
        observation.setValueJson(requestDto.getValueJson());

        Observation savedObservation = observationRepository.save(observation);
        return observationMapper.toDto(savedObservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationResponseDto> getObservationsByEncounterId(UUID encounterId) {
        if (!encounterRepository.existsByIdAndIsDeletedFalse(encounterId)) {
            throw new ResourceNotFoundException("Encounter not found with ID: " + encounterId);
        }

        return observationRepository.findByEncounterIdAndIsDeletedFalse(encounterId)
                .stream()
                .map(observationMapper::toDto)
                .toList();
    }
}
