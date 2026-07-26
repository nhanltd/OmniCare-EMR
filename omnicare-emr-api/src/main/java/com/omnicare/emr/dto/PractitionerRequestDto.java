package com.omnicare.emr.dto;

import com.omnicare.emr.entity.PractitionerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for Practitioner creation and update request payloads.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body payload for creating or updating a healthcare practitioner")
public class PractitionerRequestDto {

    @NotBlank(message = "Practitioner code is required")
    @Size(max = 50, message = "Practitioner code must not exceed 50 characters")
    @Schema(description = "Unique code or license identifier of the practitioner", example = "PRAC-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String practitionerCode;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    @Schema(description = "Full name of the practitioner", example = "Dr. John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @NotBlank(message = "Specialty is required")
    @Size(max = 100, message = "Specialty must not exceed 100 characters")
    @Schema(description = "Medical specialty or department", example = "CARDIOLOGY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String specialty;

    @NotNull(message = "Practitioner type is required")
    @Schema(description = "Role/type of healthcare practitioner", example = "DOCTOR", requiredMode = Schema.RequiredMode.REQUIRED)
    private PractitionerType practitionerType;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Schema(description = "Contact phone number", example = "+1-555-0199")
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "Work email address", example = "john.doe@omnicare.com")
    private String email;
}
