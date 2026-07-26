package com.omnicare.emr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResponseDto {

    private UUID id;
    private UUID encounterId;
    private String icd10Code;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
