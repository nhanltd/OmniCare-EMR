package com.omnicare.emr.aspect;

import com.omnicare.emr.dto.EncounterResponseDto;
import com.omnicare.emr.dto.FinalizeEncounterResponseDto;
import com.omnicare.emr.entity.AuditLog;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.repository.AuditLogRepository;
import com.omnicare.emr.repository.EncounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring AOP Aspect for automatically recording Encounter status transitions.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class EncounterAuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final EncounterRepository encounterRepository;

    @Pointcut("execution(* com.omnicare.emr.service.EncounterService.update*(..)) || " +
              "execution(* com.omnicare.emr.service.EncounterService.finalize*(..)) || " +
              "execution(* com.omnicare.emr.service.EncounterService.create*(..))")
    public void encounterStatusChangeMethods() {}

    @Around("encounterStatusChangeMethods()")
    public Object auditEncounterStatusTransition(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        UUID encounterId = extractEncounterId(args);

        String oldStatusStr = null;
        if (encounterId != null) {
            Optional<Encounter> existingEncounter = encounterRepository.findById(encounterId);
            if (existingEncounter.isPresent()) {
                oldStatusStr = existingEncounter.get().getStatus() != null ? existingEncounter.get().getStatus().name() : null;
            }
        }

        // Execute actual business method
        Object result = joinPoint.proceed();

        String newStatusStr = null;
        if (result instanceof EncounterResponseDto dto) {
            if (encounterId == null) {
                encounterId = dto.getId();
            }
            newStatusStr = dto.getStatus() != null ? dto.getStatus().name() : null;
        } else if (result instanceof FinalizeEncounterResponseDto dto) {
            if (encounterId == null) {
                encounterId = dto.getEncounterId();
            }
            newStatusStr = dto.getStatus() != null ? dto.getStatus().name() : null;
        } else if (encounterId != null) {
            Optional<Encounter> updatedEncounter = encounterRepository.findById(encounterId);
            if (updatedEncounter.isPresent()) {
                newStatusStr = updatedEncounter.get().getStatus() != null ? updatedEncounter.get().getStatus().name() : null;
            }
        }

        boolean isStatusChange = (oldStatusStr != null && newStatusStr != null && !oldStatusStr.equalsIgnoreCase(newStatusStr))
                || (oldStatusStr == null && newStatusStr != null);

        if (encounterId != null && newStatusStr != null && isStatusChange) {
            log.info("Audit Trail: Intercepted encounter status change for entityId {}: {} -> {}",
                    encounterId, oldStatusStr, newStatusStr);
            AuditLog auditLog = AuditLog.builder()
                    .entityId(encounterId)
                    .oldStatus(oldStatusStr)
                    .newStatus(newStatusStr)
                    .changedAt(Instant.now())
                    .action("ENCOUNTER_STATUS_CHANGE")
                    .build();

            auditLogRepository.save(auditLog);
        }

        return result;
    }

    private UUID extractEncounterId(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof UUID uuid) {
                return uuid;
            }
        }
        return null;
    }
}
