package com.omnicare.emr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(
    name = "diagnostic_report",
    indexes = {
        @Index(name = "idx_diagnostic_report_encounter_id", columnList = "encounter_id"),
        @Index(name = "idx_diagnostic_report_status", columnList = "status"),
        @Index(name = "idx_diagnostic_report_test_code", columnList = "test_code")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DiagnosticReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "encounter_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_diagnostic_report_encounter")
    )
    private Encounter encounter;

    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt;

    @Column(name = "result_received_at")
    private Instant resultReceivedAt;

    @Column(name = "test_code", nullable = false, length = 50)
    private String testCode;

    @Column(name = "test_name", nullable = false, length = 100)
    private String testName;

    @Column(name = "result_value", length = 255)
    private String resultValue;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Column(name = "flag", length = 20)
    private String flag;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DiagnosticReportStatus status;
}
