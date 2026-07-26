package com.omnicare.emr.repository;

import com.omnicare.emr.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Patient entity.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    /**
     * Check if a patient exists with the given identifier.
     *
     * @param identifier the patient identifier (e.g. CCCD)
     * @return true if patient exists, false otherwise
     */
    boolean existsByIdentifier(String identifier);

    /**
     * Find an active (non-soft-deleted) patient by ID.
     *
     * @param id the patient UUID
     * @return an Optional containing the patient if found and not deleted
     */
    Optional<Patient> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Check if an active patient exists by ID.
     *
     * @param id the patient UUID
     * @return true if active patient exists
     */
    boolean existsByIdAndIsDeletedFalse(UUID id);

    /**
     * Count active (non-soft-deleted) patients.
     *
     * @return count of active patients
     */
    long countByIsDeletedFalse();
}
