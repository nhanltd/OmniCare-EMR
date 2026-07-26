package com.omnicare.emr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class FinalizeEncounterRequestDto {

    @NotEmpty(message = "At least one diagnosis is required")
    @Valid
    private List<DiagnosisRequestDto> diagnoses;

    @NotEmpty(message = "At least one prescription item is required")
    @Valid
    private List<PrescriptionItemRequestDto> prescriptions;
}
