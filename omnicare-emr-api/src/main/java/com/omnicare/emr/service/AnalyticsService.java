package com.omnicare.emr.service;

import com.omnicare.emr.dto.OperationalKpiResponseDto;
import com.omnicare.emr.dto.PatientClinicalHistoryResponseDto;

import java.util.UUID;

public interface AnalyticsService {
    OperationalKpiResponseDto getOperationalKpis();
    PatientClinicalHistoryResponseDto getPatientClinicalHistory(UUID patientId);
}
