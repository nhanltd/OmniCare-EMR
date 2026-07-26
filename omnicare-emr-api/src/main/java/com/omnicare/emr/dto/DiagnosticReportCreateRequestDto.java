package com.omnicare.emr.dto;

import com.omnicare.emr.entity.DiagnosticReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class DiagnosticReportCreateRequestDto {

    @NotNull(message = "Encounter ID is required")
    private UUID encounterId;

    @NotBlank(message = "Test code is required")
    @Size(max = 50, message = "Test code must not exceed 50 characters")
    private String testCode;

    @NotBlank(message = "Test name is required")
    @Size(max = 100, message = "Test name must not exceed 100 characters")
    private String testName;

    private Instant orderedAt;

    private DiagnosticReportStatus status;
}
