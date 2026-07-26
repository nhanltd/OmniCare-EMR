package com.omnicare.emr.service;

import com.omnicare.emr.dto.EncounterRequestDto;
import com.omnicare.emr.dto.EncounterResponseDto;
import com.omnicare.emr.dto.mapper.EncounterMapper;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.repository.EncounterRepository;
import com.omnicare.emr.repository.PatientRepository;
import com.omnicare.emr.repository.PractitionerRepository;
import com.omnicare.emr.service.impl.EncounterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncounterServiceImplTest {

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PractitionerRepository practitionerRepository;

    @Mock
    private EncounterMapper encounterMapper;

    @InjectMocks
    private EncounterServiceImpl encounterService;

    private UUID patientId;
    private UUID practitionerId;
    private UUID encounterId;
    private Patient patient;
    private Practitioner practitioner;
    private Encounter encounter;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        practitionerId = UUID.randomUUID();
        encounterId = UUID.randomUUID();

        patient = Patient.builder()
                .id(patientId)
                .fullName("John Doe")
                .build();

        practitioner = Practitioner.builder()
                .id(practitionerId)
                .fullName("Dr. Smith")
                .build();

        encounter = Encounter.builder()
                .id(encounterId)
                .patient(patient)
                .practitioner(practitioner)
                .encounterDate(Instant.now())
                .status(EncounterStatus.PLANNED)
                .reason("Routine checkup")
                .build();
    }

    @Test
    void createEncounter_Success_DefaultStatusPlanned() {
        EncounterRequestDto request = EncounterRequestDto.builder()
                .patientId(patientId)
                .practitionerId(practitionerId)
                .encounterDate(Instant.now())
                .reason("Routine checkup")
                .build(); // status is null

        EncounterResponseDto expectedResponse = EncounterResponseDto.builder()
                .id(encounterId)
                .patientId(patientId)
                .patientName("John Doe")
                .practitionerId(practitionerId)
                .practitionerName("Dr. Smith")
                .status(EncounterStatus.PLANNED)
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(patientId)).thenReturn(Optional.of(patient));
        when(practitionerRepository.findByIdAndIsDeletedFalse(practitionerId)).thenReturn(Optional.of(practitioner));
        when(encounterMapper.toEntity(request)).thenReturn(Encounter.builder().build());
        when(encounterRepository.save(any(Encounter.class))).thenReturn(encounter);
        when(encounterMapper.toDto(encounter)).thenReturn(expectedResponse);

        EncounterResponseDto result = encounterService.createEncounter(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EncounterStatus.PLANNED);
        verify(encounterRepository).save(any(Encounter.class));
    }

    @Test
    void createEncounter_MissingPatient_ThrowsResourceNotFoundException() {
        EncounterRequestDto request = EncounterRequestDto.builder()
                .patientId(patientId)
                .practitionerId(practitionerId)
                .encounterDate(Instant.now())
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> encounterService.createEncounter(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void createEncounter_MissingPractitioner_ThrowsResourceNotFoundException() {
        EncounterRequestDto request = EncounterRequestDto.builder()
                .patientId(patientId)
                .practitionerId(practitionerId)
                .encounterDate(Instant.now())
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(patientId)).thenReturn(Optional.of(patient));
        when(practitionerRepository.findByIdAndIsDeletedFalse(practitionerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> encounterService.createEncounter(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Practitioner not found");
    }

    @Test
    void getEncounterById_Success() {
        EncounterResponseDto expectedResponse = EncounterResponseDto.builder()
                .id(encounterId)
                .patientId(patientId)
                .practitionerId(practitionerId)
                .status(EncounterStatus.PLANNED)
                .build();

        when(encounterRepository.findByIdAndIsDeletedFalse(encounterId)).thenReturn(Optional.of(encounter));
        when(encounterMapper.toDto(encounter)).thenReturn(expectedResponse);

        EncounterResponseDto result = encounterService.getEncounterById(encounterId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(encounterId);
    }

    @Test
    void getEncounterById_NotFound_ThrowsResourceNotFoundException() {
        when(encounterRepository.findByIdAndIsDeletedFalse(encounterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> encounterService.getEncounterById(encounterId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Encounter not found");
    }

    @Test
    void getAllEncounters_Success() {
        EncounterResponseDto responseDto = EncounterResponseDto.builder().id(encounterId).build();

        when(encounterRepository.findAllByIsDeletedFalse()).thenReturn(List.of(encounter));
        when(encounterMapper.toDto(encounter)).thenReturn(responseDto);

        List<EncounterResponseDto> results = encounterService.getAllEncounters();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(encounterId);
    }
}
