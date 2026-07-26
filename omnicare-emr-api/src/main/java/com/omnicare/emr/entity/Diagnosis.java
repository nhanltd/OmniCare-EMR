package com.omnicare.emr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(
    name = "diagnosis",
    indexes = {
        @Index(name = "idx_diagnosis_encounter_id", columnList = "encounter_id"),
        @Index(name = "idx_diagnosis_icd10_code", columnList = "icd10_code")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Diagnosis extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "encounter_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_diagnosis_encounter")
    )
    private Encounter encounter;

    @Column(name = "icd10_code", nullable = false, length = 16)
    private String icd10Code;

    @Column(name = "description", nullable = false, length = 512)
    private String description;
}
