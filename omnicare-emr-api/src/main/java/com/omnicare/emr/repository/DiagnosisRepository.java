package com.omnicare.emr.repository;

import com.omnicare.emr.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

    Optional<Diagnosis> findByIdAndIsDeletedFalse(UUID id);

    List<Diagnosis> findByEncounterIdAndIsDeletedFalse(UUID encounterId);

    List<Diagnosis> findAllByIsDeletedFalse();
}
