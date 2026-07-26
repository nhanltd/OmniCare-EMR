package com.omnicare.emr.repository;

import com.omnicare.emr.entity.Practitioner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Practitioner entity.
 */
@Repository
public interface PractitionerRepository extends JpaRepository<Practitioner, UUID> {

    /**
     * Check if a practitioner exists with the given practitioner code.
     *
     * @param practitionerCode the practitioner code (e.g., PRAC-001)
     * @return true if practitioner exists, false otherwise
     */
    boolean existsByPractitionerCode(String practitionerCode);

    /**
     * Check if a practitioner exists with the given practitioner code excluding a specific ID.
     * Useful for unique constraint verification during updates.
     *
     * @param practitionerCode the practitioner code
     * @param id the practitioner ID to exclude from match
     * @return true if another practitioner exists with the same code, false otherwise
     */
    boolean existsByPractitionerCodeAndIdNot(String practitionerCode, UUID id);

    /**
     * Find an active (non-soft-deleted) practitioner by ID.
     *
     * @param id the practitioner UUID
     * @return an Optional containing the practitioner if found and not deleted, or empty otherwise
     */
    Optional<Practitioner> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Find all active (non-soft-deleted) practitioners.
     *
     * @return list of active practitioners
     */
    List<Practitioner> findAllByIsDeletedFalse();

    /**
     * Count active (non-soft-deleted) practitioners.
     *
     * @return count of active practitioners
     */
    long countByIsDeletedFalse();
}
