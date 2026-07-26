package com.omnicare.emr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "audit_log",
    indexes = {
        @Index(name = "idx_audit_log_entity_id", columnList = "entity_id"),
        @Index(name = "idx_audit_log_changed_at", columnList = "changed_at"),
        @Index(name = "idx_audit_log_action", columnList = "action")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuditLog extends BaseEntity {

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "old_status", length = 32)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 32)
    private String newStatus;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "action", nullable = false, length = 64)
    private String action;
}
