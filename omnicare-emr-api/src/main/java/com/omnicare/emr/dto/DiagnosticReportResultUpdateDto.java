package com.omnicare.emr.dto;

import com.omnicare.emr.entity.DiagnosticReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticReportResultUpdateDto {

    @NotBlank(message = "Result value is required")
    @Size(max = 255, message = "Result value must not exceed 255 characters")
    private String resultValue;

    @Size(max = 50, message = "Unit must not exceed 50 characters")
    private String unit;

    @Size(max = 100, message = "Reference range must not exceed 100 characters")
    private String referenceRange;

    @Size(max = 20, message = "Flag must not exceed 20 characters")
    private String flag;

    private DiagnosticReportStatus status;
}
