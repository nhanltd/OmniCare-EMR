package com.omnicare.emr.dto;

import com.omnicare.emr.entity.EncounterStatus;
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
public class FinalizeEncounterResponseDto {

    private UUID encounterId;
    private EncounterStatus status;
    private Instant updatedAt;
    private List<DiagnosisResponseDto> diagnoses;
    private List<PrescriptionItemResponseDto> prescriptions;
}
