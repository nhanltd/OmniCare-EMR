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
public class PrescriptionItemResponseDto {

    private UUID id;
    private UUID encounterId;
    private String medicationName;
    private Double dosage;
    private String frequency;
    private String duration;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
