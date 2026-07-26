package com.omnicare.emr.service;

import com.omnicare.emr.dto.mapper.PatientMapper;
import com.omnicare.emr.dto.PatientRequestDto;
import com.omnicare.emr.dto.PatientResponseDto;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.exception.DuplicateResourceException;
import com.omnicare.emr.repository.PatientRepository;
import com.omnicare.emr.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    private final PatientMapper patientMapper = Mappers.getMapper(PatientMapper.class);

    private PatientServiceImpl patientService;

    private PatientRequestDto requestDto;
    private Patient savedPatient;

    @BeforeEach
    void setUp() {
        patientService = new PatientServiceImpl(patientRepository, patientMapper);

        requestDto = PatientRequestDto.builder()
                .identifier("079123456789")
                .fullName("Nguyễn Văn A")
                .gender("male")
                .birthDate(LocalDate.of(1990, 1, 1))
                .phoneNumber("+84901234567")
                .build();

        savedPatient = Patient.builder()
                .id(UUID.randomUUID())
                .identifier("079123456789")
                .fullName("Nguyễn Văn A")
                .gender("male")
                .birthDate(LocalDate.of(1990, 1, 1))
                .phoneNumber("+84901234567")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .isDeleted(false)
                .build();
    }

    @Test
    void createPatient_Success() {
        when(patientRepository.existsByIdentifier(requestDto.getIdentifier())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);

        PatientResponseDto response = patientService.createPatient(requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedPatient.getId());
        assertThat(response.getIdentifier()).isEqualTo("079123456789");
        assertThat(response.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(response.getVersion()).isEqualTo(0L);
        assertThat(response.isDeleted()).isFalse();

        verify(patientRepository).existsByIdentifier(requestDto.getIdentifier());
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    void createPatient_DuplicateIdentifier_ThrowsDuplicateResourceException() {
        when(patientRepository.existsByIdentifier(requestDto.getIdentifier())).thenReturn(true);

        assertThatThrownBy(() -> patientService.createPatient(requestDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("079123456789");

        verify(patientRepository).existsByIdentifier(requestDto.getIdentifier());
        verify(patientRepository, never()).save(any(Patient.class));
    }
}
