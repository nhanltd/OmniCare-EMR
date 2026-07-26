package com.omnicare.emr.service.impl;

import com.omnicare.emr.dto.PatientRequestDto;
import com.omnicare.emr.dto.PatientResponseDto;
import com.omnicare.emr.dto.mapper.PatientMapper;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.exception.DuplicateResourceException;
import com.omnicare.emr.repository.PatientRepository;
import com.omnicare.emr.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link PatientService}.
 */
@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Override
    @Transactional
    public PatientResponseDto createPatient(PatientRequestDto requestDto) {
        if (patientRepository.existsByIdentifier(requestDto.getIdentifier())) {
            throw new DuplicateResourceException(
                    "Patient with identifier '" + requestDto.getIdentifier() + "' already exists"
            );
        }

        Patient patient = patientMapper.toEntity(requestDto);

        Patient savedPatient = patientRepository.save(patient);

        return patientMapper.toDto(savedPatient);
    }
}
