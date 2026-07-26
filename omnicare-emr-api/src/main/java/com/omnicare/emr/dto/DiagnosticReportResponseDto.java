package com.omnicare.emr.dto;

import com.omnicare.emr.entity.DiagnosticReportStatus;
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
public class DiagnosticReportResponseDto {

    private UUID id;
    private UUID encounterId;
    private Instant orderedAt;
    private Instant resultReceivedAt;
    private String testCode;
    private String testName;
    private String resultValue;
    private String unit;
    private String referenceRange;
    private String flag;
    private DiagnosticReportStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
