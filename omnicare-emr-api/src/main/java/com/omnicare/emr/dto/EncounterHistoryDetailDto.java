package com.omnicare.emr.dto;

import com.omnicare.emr.entity.EncounterStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed record of an encounter within a patient clinical history timeline")
public class EncounterHistoryDetailDto {

    private UUID encounterId;
    private Instant encounterDate;
    private EncounterStatus status;
    private String reason;

    private UUID practitionerId;
    private String practitionerName;
    private String practitionerSpecialty;

    private List<ObservationResponseDto> observations;
    private List<DiagnosticReportResponseDto> diagnosticReports;
    private List<DiagnosisResponseDto> diagnoses;
    private List<PrescriptionItemResponseDto> prescriptions;
}