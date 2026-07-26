package com.omnicare.emr.repository;

import com.omnicare.emr.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityId(UUID entityId);

    List<AuditLog> findByEntityIdOrderByChangedAtDesc(UUID entityId);

    List<AuditLog> findByAction(String action);
}
