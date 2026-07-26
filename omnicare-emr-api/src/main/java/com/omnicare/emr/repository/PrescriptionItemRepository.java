package com.omnicare.emr.repository;

import com.omnicare.emr.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {

    Optional<PrescriptionItem> findByIdAndIsDeletedFalse(UUID id);

    List<PrescriptionItem> findByEncounterIdAndIsDeletedFalse(UUID encounterId);

    List<PrescriptionItem> findAllByIsDeletedFalse();
}
