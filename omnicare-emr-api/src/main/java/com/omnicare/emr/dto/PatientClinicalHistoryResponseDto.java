package com.omnicare.emr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Consolidated clinical history timeline for a patient")
public class PatientClinicalHistoryResponseDto {

 private PatientResponseDto patient;
 private int totalEncounters;
 private List<EncounterHistoryDetailDto> encounters;
}
