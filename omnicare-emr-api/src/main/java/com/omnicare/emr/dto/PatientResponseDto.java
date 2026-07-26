package com.omnicare.emr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for Patient creation response body.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponseDto {

    private UUID id;
    private String identifier;
    private String fullName;
    private String gender;
    private LocalDate birthDate;
    private String phoneNumber;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    @JsonProperty("isDeleted")
    private boolean isDeleted;

    @JsonProperty("isDeleted")
    public boolean isDeleted() {
        return isDeleted;
    }
}
