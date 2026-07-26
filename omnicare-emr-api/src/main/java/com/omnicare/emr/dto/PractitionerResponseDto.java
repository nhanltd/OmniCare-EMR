package com.omnicare.emr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnicare.emr.entity.PractitionerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Practitioner API response body.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload containing complete practitioner details and audit metadata")
public class PractitionerResponseDto {

    @Schema(description = "Unique primary key UUID of the practitioner", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
    private UUID id;

    @Schema(description = "Unique practitioner code", example = "PRAC-001")
    private String practitionerCode;

    @Schema(description = "Full name of the practitioner", example = "Dr. John Doe")
    private String fullName;

    @Schema(description = "Medical specialty", example = "CARDIOLOGY")
    private String specialty;

    @Schema(description = "Type/role of practitioner", example = "DOCTOR")
    private PractitionerType practitionerType;

    @Schema(description = "Contact phone number", example = "+1-555-0199")
    private String phone;

    @Schema(description = "Work email address", example = "john.doe@omnicare.com")
    private String email;

    @Schema(description = "Creation timestamp (UTC)", example = "2026-07-25T12:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp (UTC)", example = "2026-07-25T12:00:00Z")
    private Instant updatedAt;

    @Schema(description = "Optimistic locking version", example = "0")
    private Long version;

    @JsonProperty("isDeleted")
    @Schema(description = "Soft deletion status flag", example = "false")
    private boolean isDeleted;
}
