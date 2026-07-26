package com.omnicare.emr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload containing operational KPIs and clinical analytics")
public class OperationalKpiResponseDto {

    @Schema(description = "Average Turnaround Time in minutes for diagnostic reports", example = "45.5")
    private Double avgTurnaroundTimeMinutes;

    @Schema(description = "Total count of registered active patients", example = "150")
    private long totalPatients;

    @Schema(description = "Total count of registered active practitioners", example = "25")
    private long totalPractitioners;

    @Schema(description = "Total count of encounters recorded", example = "320")
    private long totalEncounters;

    @Schema(description = "Encounter count distribution by status")
    private Map<String, Long> encounterStatusCounts;

    @Schema(description = "Top ICD-10 diagnosis frequency distribution")
    private Map<String, Long> topIcd10Diagnoses;
}