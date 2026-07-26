package com.omnicare.emr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisRequestDto {

    @NotBlank(message = "ICD-10 code is required")
    @Size(max = 16, message = "ICD-10 code must not exceed 16 characters")
    private String icd10Code;

    @NotBlank(message = "Description is required")
    @Size(max = 512, message = "Description must not exceed 512 characters")
    private String description;
}
