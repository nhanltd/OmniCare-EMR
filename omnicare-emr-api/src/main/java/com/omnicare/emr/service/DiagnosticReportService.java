package com.omnicare.emr.service;

import com.omnicare.emr.dto.DiagnosticReportCreateRequestDto;
import com.omnicare.emr.dto.DiagnosticReportResponseDto;
import com.omnicare.emr.dto.DiagnosticReportResultUpdateDto;

import java.util.List;
import java.util.UUID;

public interface DiagnosticReportService {

    DiagnosticReportResponseDto createDiagnosticReport(DiagnosticReportCreateRequestDto requestDto);

    DiagnosticReportResponseDto updateDiagnosticReportResults(UUID id, DiagnosticReportResultUpdateDto resultDto);

    DiagnosticReportResponseDto getDiagnosticReportById(UUID id);

    List<DiagnosticReportResponseDto> getDiagnosticReportsByEncounterId(UUID encounterId);
}
