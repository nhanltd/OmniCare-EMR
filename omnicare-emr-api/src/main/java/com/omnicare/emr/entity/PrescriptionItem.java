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
    name = "prescription_item",
    indexes = {
        @Index(name = "idx_prescription_item_encounter_id", columnList = "encounter_id")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PrescriptionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "encounter_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_prescription_item_encounter")
    )
    private Encounter encounter;

    @Column(name = "medication_name", nullable = false, length = 255)
    private String medicationName;

    @Column(name = "dosage", nullable = false)
    private Double dosage;

    @Column(name = "frequency", nullable = false, length = 100)
    private String frequency;

    @Column(name = "duration", nullable = false, length = 100)
    private String duration;
}
