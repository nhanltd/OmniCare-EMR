package com.omnicare.emr.dto;

import com.omnicare.emr.entity.EncounterStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Encounter response payload.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterResponseDto {

    private UUID id;
    private UUID patientId;
    private String patientName;
    private UUID practitionerId;
    private String practitionerName;
    private Instant encounterDate;
    private EncounterStatus status;
    private String reason;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
