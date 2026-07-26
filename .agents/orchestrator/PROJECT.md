# Project: OmniCare EMR

## Architecture
- **Framework**: Spring Boot 3.x, Java 17, Spring Data JPA, Hibernate 6, Flyway Migration, PostgreSQL, Lombok, Jakarta Validation, MapStruct, OpenAPI (Swagger UI), Spring AOP (`@Aspect`).
- **Package Structure**: `com.omnicare.emr`
  - `aspect`: Spring AOP aspects (`EncounterAuditAspect`)
  - `config`: JPA, OpenApi & AOP configuration
  - `controller`: REST API controllers (`/api/v1/patients`, `/api/v1/practitioners`, `/api/v1/encounters`, `/api/v1/observations`, `/api/v1/diagnostic-reports`, `/api/v1/encounters/{id}/finalize`)
  - `dto`: Request & Response DTOs, Mappers (`DiagnosticReportResultDto`, `FinalizeEncounterRequestDto`, `DiagnosisDto`, `PrescriptionItemDto`, etc.)
  - `entity`: JPA Domain Entities (`BaseEntity`, `Patient`, `Practitioner`, `Encounter`, `Observation`, `DiagnosticReport`, `Diagnosis`, `PrescriptionItem`, `AuditLog`, Enums: `PractitionerType`, `EncounterStatus`, `DiagnosticReportStatus`)
  - `exception`: Custom exceptions & `GlobalExceptionHandler` (RFC 7807)
  - `repository`: Spring Data JPA Repositories (`PatientRepository`, `PractitionerRepository`, `EncounterRepository`, `ObservationRepository`, `DiagnosticReportRepository`, `DiagnosisRepository`, `PrescriptionItemRepository`, `AuditLogRepository`)
  - `service` & `service.impl`: Business logic layer (`PatientService`, `PractitionerService`, `EncounterService`, `ObservationService`, `DiagnosticReportService`, `AuditLogService`)

## Milestones

| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Patient Infrastructure & Core API | BaseEntity, Patient, Docker, Patient API | None | DONE |
| P1-M1..M4 | Phase 1 Practitioner Module | Flyway V2, Practitioner entity/CRUD, OpenAPI | M1 | DONE |
| P2-M1..M4 | Phase 2 Clinical Core Module | Flyway V3, Encounter, Observation (JSONB), APIs, Validation | P1-M4 | DONE |
| P3-M1 | LIS Webhook & DiagnosticReport Entity | Flyway V4, `DiagnosticReport` entity, repository, service, `PUT /api/v1/diagnostic-reports/{id}/results` | P2-M4 | PLANNED |
| P3-M2 | Transactional Finalize API & Rollback | `Diagnosis`, `PrescriptionItem` entities, `POST /api/v1/encounters/{id}/finalize`, `@Transactional` rollback logic | P3-M1 | PLANNED |
| P3-M3 | Audit Trail via Spring AOP | `AuditLog` entity, Spring `@Aspect` intercepting Encounter status changes, `audit_log` insertion | P3-M2 | PLANNED |
| P3-M4 | Integration Test Suite & Forensic Audit | Unit/Integration tests for LIS webhook, Transaction Rollback, AOP Audit logging, Reviewers, Challengers, Auditor | P3-M3 | PLANNED |

## Interface Contracts
### `DiagnosticReportController` (`/api/v1/diagnostic-reports`)
- `PUT /api/v1/diagnostic-reports/{id}/results` -> 200 OK (`DiagnosticReportResponseDto` with `resultReceivedAt` set, test details updated)
- Report Not Found -> 404 Not Found (`ProblemDetail`)

### `EncounterController` (`/api/v1/encounters`)
- `POST /api/v1/encounters/{id}/finalize` -> 200/201 OK (`EncounterResponseDto` with status `FINISHED`, diagnoses & prescriptions saved)
- Validation failure (e.g. prescription item dosage <= 0) -> 400 Bad Request (`ProblemDetail`), transactional rollback (0 diagnoses saved)
- Encounter Not Found -> 404 Not Found (`ProblemDetail`)

### Audit Trail (Aspect)
- Any Encounter status transition (e.g. `PLANNED` -> `FINISHED` or `CANCELLED`) automatically creates an `AuditLog` entry in `audit_log` with `entityId`, `oldStatus`, `newStatus`, `changedAt`, `action`.
