package com.omnicare.emr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.omnicare.emr.service.impl.ObservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservationServiceImplTest {

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private ObservationMapper observationMapper;

    @InjectMocks
    private ObservationServiceImpl observationService;

    private ObjectMapper objectMapper;
    private UUID encounterId;
    private UUID observationId;
    private Encounter activeEncounter;
    private Encounter cancelledEncounter;
    private JsonNode vitalsJson;
    private Observation observation;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        encounterId = UUID.randomUUID();
        observationId = UUID.randomUUID();

        vitalsJson = objectMapper.readTree("{\"bloodPressure\": \"120/80\", \"heartRate\": 75, \"temp\": 37.0}");

        activeEncounter = Encounter.builder()
                .id(encounterId)
                .status(EncounterStatus.IN_PROGRESS)
                .build();

        cancelledEncounter = Encounter.builder()
                .id(encounterId)
                .status(EncounterStatus.CANCELLED)
                .build();

        observation = Observation.builder()
                .id(observationId)
                .encounter(activeEncounter)
                .valueJson(vitalsJson)
                .build();
    }

    @Test
    void createObservation_Success() {
        ObservationRequestDto request = ObservationRequestDto.builder()
                .encounterId(encounterId)
                .valueJson(vitalsJson)
                .build();

        ObservationResponseDto responseDto = ObservationResponseDto.builder()
                .id(observationId)
                .encounterId(encounterId)
                .valueJson(vitalsJson)
                .build();

        when(encounterRepository.findByIdAndIsDeletedFalse(encounterId)).thenReturn(Optional.of(activeEncounter));
        when(observationMapper.toEntity(request)).thenReturn(Observation.builder().build());
        when(observationRepository.save(any(Observation.class))).thenReturn(observation);
        when(observationMapper.toDto(observation)).thenReturn(responseDto);

        ObservationResponseDto result = observationService.createObservation(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(observationId);
        assertThat(result.getValueJson().get("heartRate").asInt()).isEqualTo(75);
        verify(observationRepository).save(any(Observation.class));
    }

    @Test
    void createObservation_MissingEncounter_ThrowsResourceNotFoundException() {
        ObservationRequestDto request = ObservationRequestDto.builder()
                .encounterId(encounterId)
                .valueJson(vitalsJson)
                .build();

        when(encounterRepository.findByIdAndIsDeletedFalse(encounterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> observationService.createObservation(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Encounter not found");
    }

    @Test
    void createObservation_CancelledEncounter_ThrowsEncounterCancelledException() {
        ObservationRequestDto request = ObservationRequestDto.builder()
                .encounterId(encounterId)
                .valueJson(vitalsJson)
                .build();

        when(encounterRepository.findByIdAndIsDeletedFalse(encounterId)).thenReturn(Optional.of(cancelledEncounter));

        assertThatThrownBy(() -> observationService.createObservation(request))
                .isInstanceOf(EncounterCancelledException.class)
                .hasMessageContaining("Cannot record observation for cancelled encounter");
    }

    @Test
    void getObservationsByEncounterId_Success() {
        ObservationResponseDto responseDto = ObservationResponseDto.builder()
                .id(observationId)
                .encounterId(encounterId)
                .valueJson(vitalsJson)
                .build();

        when(encounterRepository.existsByIdAndIsDeletedFalse(encounterId)).thenReturn(true);
        when(observationRepository.findByEncounterIdAndIsDeletedFalse(encounterId)).thenReturn(List.of(observation));
        when(observationMapper.toDto(observation)).thenReturn(responseDto);

        List<ObservationResponseDto> results = observationService.getObservationsByEncounterId(encounterId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(observationId);
    }

    @Test
    void getObservationsByEncounterId_EncounterNotFound_ThrowsResourceNotFoundException() {
        when(encounterRepository.existsByIdAndIsDeletedFalse(encounterId)).thenReturn(false);

        assertThatThrownBy(() -> observationService.getObservationsByEncounterId(encounterId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Encounter not found");
    }
}
