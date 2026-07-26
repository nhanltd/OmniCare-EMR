package com.omnicare.emr.repository;

import com.omnicare.emr.entity.Observation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Observation entity.
 */
@Repository
public interface ObservationRepository extends JpaRepository<Observation, UUID> {

    /**
     * Find an active (non-deleted) observation by ID.
     */
    Optional<Observation> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Find all active observations associated with a given encounter ID.
     */
    List<Observation> findByEncounterIdAndIsDeletedFalse(UUID encounterId);

    /**
     * Find all active (non-deleted) observations.
     */
    List<Observation> findAllByIsDeletedFalse();
}
