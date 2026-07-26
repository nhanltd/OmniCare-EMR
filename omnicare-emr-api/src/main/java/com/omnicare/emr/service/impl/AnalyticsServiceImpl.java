package com.omnicare.emr.service.impl;

import com.omnicare.emr.dto.DiagnosisResponseDto;
import com.omnicare.emr.dto.DiagnosticReportResponseDto;
import com.omnicare.emr.dto.EncounterHistoryDetailDto;
import com.omnicare.emr.dto.ObservationResponseDto;
import com.omnicare.emr.dto.OperationalKpiResponseDto;
import com.omnicare.emr.dto.PatientClinicalHistoryResponseDto;
import com.omnicare.emr.dto.PatientResponseDto;
import com.omnicare.emr.dto.PrescriptionItemResponseDto;
import com.omnicare.emr.dto.mapper.DiagnosisMapper;
import com.omnicare.emr.dto.mapper.DiagnosticReportMapper;
import com.omnicare.emr.dto.mapper.ObservationMapper;
import com.omnicare.emr.dto.mapper.PatientMapper;
import com.omnicare.emr.dto.mapper.PrescriptionItemMapper;
import com.omnicare.emr.entity.Diagnosis;
import com.omnicare.emr.entity.DiagnosticReport;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Observation;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.entity.PrescriptionItem;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.repository.DiagnosisRepository;
import com.omnicare.emr.repository.DiagnosticReportRepository;
import com.omnicare.emr.repository.EncounterRepository;
import com.omnicare.emr.repository.ObservationRepository;
import com.omnicare.emr.repository.PatientRepository;
import com.omnicare.emr.repository.PractitionerRepository;
import com.omnicare.emr.repository.PrescriptionItemRepository;
import com.omnicare.emr.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final EncounterRepository encounterRepository;
    private final ObservationRepository observationRepository;
    private final DiagnosticReportRepository diagnosticReportRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;

    private final PatientMapper patientMapper = Mappers.getMapper(PatientMapper.class);
    private final ObservationMapper observationMapper = Mappers.getMapper(ObservationMapper.class);
    private final DiagnosticReportMapper diagnosticReportMapper = Mappers.getMapper(DiagnosticReportMapper.class);
    private final DiagnosisMapper diagnosisMapper = Mappers.getMapper(DiagnosisMapper.class);
    private final PrescriptionItemMapper prescriptionItemMapper = Mappers.getMapper(PrescriptionItemMapper.class);

    @Override
    public OperationalKpiResponseDto getOperationalKpis() {
        long totalPatients = patientRepository.countByIsDeletedFalse();
        long totalPractitioners = practitionerRepository.countByIsDeletedFalse();

        List<Encounter> allEncounters = encounterRepository.findAllByIsDeletedFalse();
        long totalEncounters = allEncounters.size();

        Map<String, Long> statusCounts = new HashMap<>();
        for (EncounterStatus status : EncounterStatus.values()) {
            statusCounts.put(status.name(), 0L);
        }
        for (Encounter encounter : allEncounters) {
            String statusName = encounter.getStatus().name();
            statusCounts.put(statusName, statusCounts.getOrDefault(statusName, 0L) + 1L);
        }

        List<DiagnosticReport> allReports = diagnosticReportRepository.findAllByIsDeletedFalse();
        double totalDurationMinutes = 0.0;
        long completedReportsCount = 0;

        for (DiagnosticReport report : allReports) {
            if (report.getOrderedAt() != null && report.getResultReceivedAt() != null) {
                Duration duration = Duration.between(report.getOrderedAt(), report.getResultReceivedAt());
                totalDurationMinutes += duration.toSeconds() / 60.0;
                completedReportsCount++;
            }
        }

        Double avgTat = completedReportsCount > 0 ? (totalDurationMinutes / completedReportsCount) : null;

        List<Diagnosis> allDiagnoses = diagnosisRepository.findAllByIsDeletedFalse();
        Map<String, Long> icd10Counts = new HashMap<>();
        for (Diagnosis diagnosis : allDiagnoses) {
            String code = diagnosis.getIcd10Code();
            icd10Counts.put(code, icd10Counts.getOrDefault(code, 0L) + 1L);
        }

        return OperationalKpiResponseDto.builder()
                .avgTurnaroundTimeMinutes(avgTat)
                .totalPatients(totalPatients)
                .totalPractitioners(totalPractitioners)
                .totalEncounters(totalEncounters)
                .encounterStatusCounts(statusCounts)
                .topIcd10Diagnoses(icd10Counts)
                .build();
    }

    @Override
    public PatientClinicalHistoryResponseDto getPatientClinicalHistory(UUID patientId) {
        Patient patient = patientRepository.findByIdAndIsDeletedFalse(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        PatientResponseDto patientDto = patientMapper.toDto(patient);
        List<Encounter> encounters = encounterRepository.findByPatientIdAndIsDeletedFalseOrderByEncounterDateDesc(patientId);

        List<EncounterHistoryDetailDto> encounterDetails = new ArrayList<>();

        for (Encounter encounter : encounters) {
            UUID encId = encounter.getId();

            List<Observation> obsList = observationRepository.findByEncounterIdAndIsDeletedFalse(encId);
            List<ObservationResponseDto> obsDtos = obsList.stream().map(observationMapper::toDto).toList();

            List<DiagnosticReport> reports = diagnosticReportRepository.findByEncounterIdAndIsDeletedFalse(encId);
            List<DiagnosticReportResponseDto> reportDtos = reports.stream().map(diagnosticReportMapper::toDto).toList();

            List<Diagnosis> diagnoses = diagnosisRepository.findByEncounterIdAndIsDeletedFalse(encId);
            List<DiagnosisResponseDto> diagDtos = diagnoses.stream().map(diagnosisMapper::toDto).toList();

            List<PrescriptionItem> prescriptions = prescriptionItemRepository.findByEncounterIdAndIsDeletedFalse(encId);
            List<PrescriptionItemResponseDto> prescDtos = prescriptions.stream().map(prescriptionItemMapper::toDto).toList();

 EncounterHistoryDetailDto detail = EncounterHistoryDetailDto.builder()
 .encounterId(encId)
 .encounterDate(encounter.getEncounterDate())
 .status(encounter.getStatus())
 .reason(encounter.getReason())
 .practitionerId(encounter.getPractitioner().getId())
 .practitionerName(encounter.getPractitioner().getFullName())
 .practitionerSpecialty(encounter.getPractitioner().getSpecialty())
 .observations(obsDtos)
 .diagnosticReports(reportDtos)
 .diagnoses(diagDtos)
 .prescriptions(prescDtos)
 .build();

 encounterDetails.add(detail);
 }

 return PatientClinicalHistoryResponseDto.builder()
 .patient(patientDto)
 .totalEncounters(encounterDetails.size())
 .encounters(encounterDetails)
 .build();
 }
}
