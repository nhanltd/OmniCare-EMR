package com.omnicare.emr.service.impl;

import com.omnicare.emr.dto.PractitionerRequestDto;
import com.omnicare.emr.dto.PractitionerResponseDto;
import com.omnicare.emr.dto.mapper.PractitionerMapper;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.exception.DuplicateResourceException;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.repository.PractitionerRepository;
import com.omnicare.emr.service.PractitionerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link PractitionerService}.
 */
@Service
@RequiredArgsConstructor
public class PractitionerServiceImpl implements PractitionerService {

    private final PractitionerRepository practitionerRepository;
    private final PractitionerMapper practitionerMapper;

    @Override
    @Transactional
    public PractitionerResponseDto createPractitioner(PractitionerRequestDto requestDto) {
        if (practitionerRepository.existsByPractitionerCode(requestDto.getPractitionerCode())) {
            throw new DuplicateResourceException(
                    "Practitioner with code '" + requestDto.getPractitionerCode() + "' already exists"
            );
        }

        Practitioner practitioner = practitionerMapper.toEntity(requestDto);
        Practitioner savedPractitioner = practitionerRepository.save(practitioner);

        return practitionerMapper.toDto(savedPractitioner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PractitionerResponseDto> getAllPractitioners() {
        return practitionerRepository.findAllByIsDeletedFalse()
                .stream()
                .map(practitionerMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PractitionerResponseDto getPractitionerById(UUID id) {
        Practitioner practitioner = practitionerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found with ID: " + id));

        return practitionerMapper.toDto(practitioner);
    }

    @Override
    @Transactional
    public PractitionerResponseDto updatePractitioner(UUID id, PractitionerRequestDto requestDto) {
        Practitioner practitioner = practitionerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found with ID: " + id));

        if (practitionerRepository.existsByPractitionerCodeAndIdNot(requestDto.getPractitionerCode(), id)) {
            throw new DuplicateResourceException(
                    "Practitioner with code '" + requestDto.getPractitionerCode() + "' already exists"
            );
        }

        practitionerMapper.updateEntityFromDto(requestDto, practitioner);
        Practitioner updatedPractitioner = practitionerRepository.save(practitioner);

        return practitionerMapper.toDto(updatedPractitioner);
    }

    @Override
    @Transactional
    public void deletePractitioner(UUID id) {
        Practitioner practitioner = practitionerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found with ID: " + id));

        practitioner.setDeleted(true);
        practitionerRepository.save(practitioner);
    }
}
