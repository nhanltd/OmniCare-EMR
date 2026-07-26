package com.omnicare.emr.service;

import com.omnicare.emr.dto.PatientRequestDto;
import com.omnicare.emr.dto.PatientResponseDto;

/**
 * Service interface for Patient domain operations.
 */
public interface PatientService {

    /**
     * Creates a new patient record after checking for identifier uniqueness.
     *
     * @param requestDto patient creation details
     * @return created patient response details
     */
    PatientResponseDto createPatient(PatientRequestDto requestDto);
}
