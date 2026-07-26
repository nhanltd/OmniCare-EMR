package com.omnicare.emr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO for Patient creation request payload.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequestDto {

    @NotBlank(message = "Identifier is required")
    @Size(min = 9, max = 20, message = "Identifier must be between 9 and 20 characters")
    private String identifier;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Size(max = 10, message = "Gender must not exceed 10 characters")
    private String gender;

    @PastOrPresent(message = "Birth date must be in the past or present")
    private LocalDate birthDate;

    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phoneNumber;
}
