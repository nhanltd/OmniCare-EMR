package com.omnicare.emr.service.impl;

import com.omnicare.emr.dto.DiagnosticReportCreateRequestDto;
import com.omnicare.emr.dto.DiagnosticReportResponseDto;
import com.omnicare.emr.dto.DiagnosticReportResultUpdateDto;
import com.omnicare.emr.dto.mapper.DiagnosticReportMapper;
import com.omnicare.emr.entity.DiagnosticReport;
import com.omnicare.emr.entity.DiagnosticReportStatus;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.exception.EncounterCancelledException;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.repository.DiagnosticReportRepository;
import com.omnicare.emr.repository.EncounterRepository;
import com.omnicare.emr.service.DiagnosticReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiagnosticReportServiceImpl implements DiagnosticReportService {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final EncounterRepository encounterRepository;
    private final DiagnosticReportMapper diagnosticReportMapper;

    @Override
    @Transactional
    public DiagnosticReportResponseDto createDiagnosticReport(DiagnosticReportCreateRequestDto requestDto) {
        Encounter encounter = encounterRepository.findByIdAndIsDeletedFalse(requestDto.getEncounterId())
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + requestDto.getEncounterId()));

        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new EncounterCancelledException("Cannot create diagnostic report for cancelled encounter with ID: " + encounter.getId());
        }

        DiagnosticReport report = diagnosticReportMapper.toEntity(requestDto);
        report.setEncounter(encounter);
        if (report.getOrderedAt() == null) {
            report.setOrderedAt(Instant.now());
        }
        if (report.getStatus() == null) {
            report.setStatus(DiagnosticReportStatus.ORDERED);
        }

        DiagnosticReport savedReport = diagnosticReportRepository.save(report);
        return diagnosticReportMapper.toDto(savedReport);
    }

    @Override
    @Transactional
    public DiagnosticReportResponseDto updateDiagnosticReportResults(UUID id, DiagnosticReportResultUpdateDto resultDto) {
        DiagnosticReport report = diagnosticReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic report not found with ID: " + id));

        if (report.getEncounter().getStatus() == EncounterStatus.CANCELLED) {
            throw new EncounterCancelledException("Cannot update diagnostic report results for cancelled encounter with ID: " + report.getEncounter().getId());
        }

        report.setResultValue(resultDto.getResultValue());
        report.setUnit(resultDto.getUnit());
        report.setReferenceRange(resultDto.getReferenceRange());
        report.setFlag(resultDto.getFlag());
        if (resultDto.getStatus() != null) {
            report.setStatus(resultDto.getStatus());
        } else {
            report.setStatus(DiagnosticReportStatus.FINAL);
        }
        report.setResultReceivedAt(Instant.now());

        DiagnosticReport updatedReport = diagnosticReportRepository.save(report);
        return diagnosticReportMapper.toDto(updatedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public DiagnosticReportResponseDto getDiagnosticReportById(UUID id) {
        DiagnosticReport report = diagnosticReportRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic report not found with ID: " + id));

        return diagnosticReportMapper.toDto(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosticReportResponseDto> getDiagnosticReportsByEncounterId(UUID encounterId) {
        if (!encounterRepository.existsByIdAndIsDeletedFalse(encounterId)) {
            throw new ResourceNotFoundException("Encounter not found with ID: " + encounterId);
        }

        return diagnosticReportRepository.findByEncounterIdAndIsDeletedFalse(encounterId)
                .stream()
                .map(diagnosticReportMapper::toDto)
                .toList();
    }
}
