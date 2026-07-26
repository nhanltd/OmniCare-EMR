package com.omnicare.emr.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO for recording an Observation (vitals/clinical data).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationRequestDto {

    @NotNull(message = "Encounter ID is required")
    private UUID encounterId;

    @NotNull(message = "Observation JSON payload is required")
    private JsonNode valueJson;
}
