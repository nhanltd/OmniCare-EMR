package com.omnicare.emr.repository;

import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Encounter entity.
 */
@Repository
public interface EncounterRepository extends JpaRepository<Encounter, UUID> {

    /**
     * Find an active (non-deleted) encounter by ID.
     */
    Optional<Encounter> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Find all active encounters for a specific patient.
     */
    List<Encounter> findByPatientIdAndIsDeletedFalse(UUID patientId);

    /**
     * Find all active encounters for a specific patient ordered by encounterDate descending.
     */
    List<Encounter> findByPatientIdAndIsDeletedFalseOrderByEncounterDateDesc(UUID patientId);

    /**
     * Find all active encounters for a specific practitioner.
     */
    List<Encounter> findByPractitionerIdAndIsDeletedFalse(UUID practitionerId);

    /**
     * Find all active encounters by status.
     */
    List<Encounter> findByStatusAndIsDeletedFalse(EncounterStatus status);

    /**
     * Find all active (non-deleted) encounters.
     */
    List<Encounter> findAllByIsDeletedFalse();

    /**
     * Check if an active encounter exists by ID.
     */
    boolean existsByIdAndIsDeletedFalse(UUID id);
}
