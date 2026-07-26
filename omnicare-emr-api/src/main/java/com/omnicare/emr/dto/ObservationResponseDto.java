package com.omnicare.emr.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Observation response payload.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationResponseDto {

    private UUID id;
    private UUID encounterId;
    private JsonNode valueJson;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
