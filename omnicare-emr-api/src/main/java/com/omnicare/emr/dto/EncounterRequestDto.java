package com.omnicare.emr.dto;

import com.omnicare.emr.entity.EncounterStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for creating an Encounter.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterRequestDto {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Practitioner ID is required")
    private UUID practitionerId;

    @NotNull(message = "Encounter date is required")
    private Instant encounterDate;

    private EncounterStatus status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
