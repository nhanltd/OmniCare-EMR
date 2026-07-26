package com.omnicare.emr.service;

import com.omnicare.emr.dto.PractitionerRequestDto;
import com.omnicare.emr.dto.PractitionerResponseDto;
import com.omnicare.emr.dto.mapper.PractitionerMapper;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.entity.PractitionerType;
import com.omnicare.emr.exception.DuplicateResourceException;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.repository.PractitionerRepository;
import com.omnicare.emr.service.impl.PractitionerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PractitionerServiceImplTest {

    @Mock
    private PractitionerRepository practitionerRepository;

    private final PractitionerMapper practitionerMapper = Mappers.getMapper(PractitionerMapper.class);

    private PractitionerServiceImpl practitionerService;

    private UUID sampleId;
    private PractitionerRequestDto requestDto;
    private Practitioner existingPractitioner;

    @BeforeEach
    void setUp() {
        practitionerService = new PractitionerServiceImpl(practitionerRepository, practitionerMapper);

        sampleId = UUID.randomUUID();

        requestDto = PractitionerRequestDto.builder()
                .practitionerCode("PRAC-100")
                .fullName("Dr. Alice Smith")
                .specialty("CARDIOLOGY")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+1-555-0199")
                .email("alice.smith@omnicare.com")
                .build();

        existingPractitioner = Practitioner.builder()
                .id(sampleId)
                .practitionerCode("PRAC-100")
                .fullName("Dr. Alice Smith")
                .specialty("CARDIOLOGY")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+1-555-0199")
                .email("alice.smith@omnicare.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .isDeleted(false)
                .build();
    }

    @Test
    void createPractitioner_Success() {
        when(practitionerRepository.existsByPractitionerCode(requestDto.getPractitionerCode())).thenReturn(false);
        when(practitionerRepository.save(any(Practitioner.class))).thenReturn(existingPractitioner);

        PractitionerResponseDto response = practitionerService.createPractitioner(requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(sampleId);
        assertThat(response.getPractitionerCode()).isEqualTo("PRAC-100");
        assertThat(response.getFullName()).isEqualTo("Dr. Alice Smith");
        assertThat(response.getSpecialty()).isEqualTo("CARDIOLOGY");
        assertThat(response.getPractitionerType()).isEqualTo(PractitionerType.DOCTOR);
        assertThat(response.isDeleted()).isFalse();

        verify(practitionerRepository).existsByPractitionerCode(requestDto.getPractitionerCode());
        verify(practitionerRepository).save(any(Practitioner.class));
    }

    @Test
    void createPractitioner_DuplicateCode_ThrowsDuplicateResourceException() {
        when(practitionerRepository.existsByPractitionerCode(requestDto.getPractitionerCode())).thenReturn(true);

        assertThatThrownBy(() -> practitionerService.createPractitioner(requestDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("PRAC-100");

        verify(practitionerRepository).existsByPractitionerCode(requestDto.getPractitionerCode());
        verify(practitionerRepository, never()).save(any(Practitioner.class));
    }

    @Test
    void getAllPractitioners_ReturnsActiveList() {
        when(practitionerRepository.findAllByIsDeletedFalse()).thenReturn(List.of(existingPractitioner));

        List<PractitionerResponseDto> results = practitionerService.getAllPractitioners();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPractitionerCode()).isEqualTo("PRAC-100");
        verify(practitionerRepository).findAllByIsDeletedFalse();
    }

    @Test
    void getPractitionerById_Success() {
        when(practitionerRepository.findByIdAndIsDeletedFalse(sampleId)).thenReturn(Optional.of(existingPractitioner));

        PractitionerResponseDto response = practitionerService.getPractitionerById(sampleId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(sampleId);
        assertThat(response.getPractitionerCode()).isEqualTo("PRAC-100");
        verify(practitionerRepository).findByIdAndIsDeletedFalse(sampleId);
    }

    @Test
    void getPractitionerById_NotFound_ThrowsResourceNotFoundException() {
        when(practitionerRepository.findByIdAndIsDeletedFalse(sampleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> practitionerService.getPractitionerById(sampleId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(sampleId.toString());

        verify(practitionerRepository).findByIdAndIsDeletedFalse(sampleId);
    }

    @Test
    void updatePractitioner_Success() {
        PractitionerRequestDto updateDto = PractitionerRequestDto.builder()
                .practitionerCode("PRAC-100-UPDATED")
                .fullName("Dr. Alice Smith Updated")
                .specialty("CARDIOLOGY")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+1-555-9999")
                .email("alice.updated@omnicare.com")
                .build();

        Practitioner updatedPractitioner = Practitioner.builder()
                .id(sampleId)
                .practitionerCode("PRAC-100-UPDATED")
                .fullName("Dr. Alice Smith Updated")
                .specialty("CARDIOLOGY")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+1-555-9999")
                .email("alice.updated@omnicare.com")
                .createdAt(existingPractitioner.getCreatedAt())
                .updatedAt(Instant.now())
                .version(1L)
                .isDeleted(false)
                .build();

        when(practitionerRepository.findByIdAndIsDeletedFalse(sampleId)).thenReturn(Optional.of(existingPractitioner));
        when(practitionerRepository.existsByPractitionerCodeAndIdNot("PRAC-100-UPDATED", sampleId)).thenReturn(false);
        when(practitionerRepository.save(any(Practitioner.class))).thenReturn(updatedPractitioner);

        PractitionerResponseDto response = practitionerService.updatePractitioner(sampleId, updateDto);

        assertThat(response).isNotNull();
        assertThat(response.getPractitionerCode()).isEqualTo("PRAC-100-UPDATED");
        assertThat(response.getFullName()).isEqualTo("Dr. Alice Smith Updated");

        verify(practitionerRepository).findByIdAndIsDeletedFalse(sampleId);
        verify(practitionerRepository).existsByPractitionerCodeAndIdNot("PRAC-100-UPDATED", sampleId);
        verify(practitionerRepository).save(any(Practitioner.class));
    }

    @Test
    void updatePractitioner_NotFound_ThrowsResourceNotFoundException() {
        when(practitionerRepository.findByIdAndIsDeletedFalse(sampleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> practitionerService.updatePractitioner(sampleId, requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(sampleId.toString());

        verify(practitionerRepository).findByIdAndIsDeletedFalse(sampleId);
        verify(practitionerRepository, never()).save(any(Practitioner.class));
    }

    @Test
    void updatePractitioner_DuplicateCode_ThrowsDuplicateResourceException() {
        when(practitionerRepository.findByIdAndIsDeletedFalse(sampleId)).thenReturn(Optional.of(existingPractitioner));
        when(practitionerRepository.existsByPractitionerCodeAndIdNot(requestDto.getPractitionerCode(), sampleId)).thenReturn(true);

        assertThatThrownBy(() -> practitionerService.updatePractitioner(sampleId, requestDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(requestDto.getPractitionerCode());

        verify(practitionerRepository).findByIdAndIsDeletedFalse(sampleId);
        verify(practitionerRepository).existsByPractitionerCodeAndIdNot(requestDto.getPractitionerCode(), sampleId);
        verify(practitionerRepository, never()).save(any(Practitioner.class));
    }

    @Test
    void deletePractitioner_Success() {
        when(practitionerRepository.findByIdAndIsDeletedFalse(sampleId)).thenReturn(Optional.of(existingPractitioner));
        when(practitionerRepository.save(any(Practitioner.class))).thenReturn(existingPractitioner);

        practitionerService.deletePractitioner(sampleId);

        assertThat(existingPractitioner.isDeleted()).isTrue();
        verify(practitionerRepository).findByIdAndIsDeletedFalse(sampleId);
        verify(practitionerRepository).save(existingPractitioner);
    }

    @Test
    void deletePractitioner_NotFound_ThrowsResourceNotFoundException() {
        when(practitionerRepository.findByIdAndIsDeletedFalse(sampleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> practitionerService.deletePractitioner(sampleId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(sampleId.toString());

        verify(practitionerRepository).findByIdAndIsDeletedFalse(sampleId);
        verify(practitionerRepository, never()).save(any(Practitioner.class));
    }
}
