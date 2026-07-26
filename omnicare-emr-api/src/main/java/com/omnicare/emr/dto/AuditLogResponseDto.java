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
public class AuditLogResponseDto {

    private UUID id;
    private UUID entityId;
    private String oldStatus;
    private String newStatus;
    private Instant changedAt;
    private String action;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
