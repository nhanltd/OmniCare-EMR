package com.omnicare.emr.repository;

import com.omnicare.emr.entity.DiagnosticReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiagnosticReportRepository extends JpaRepository<DiagnosticReport, UUID> {

    Optional<DiagnosticReport> findByIdAndIsDeletedFalse(UUID id);

    List<DiagnosticReport> findByEncounterIdAndIsDeletedFalse(UUID encounterId);

    List<DiagnosticReport> findAllByIsDeletedFalse();
}
