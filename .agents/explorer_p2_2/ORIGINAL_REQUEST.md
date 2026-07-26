## 2026-07-25T08:08:13Z
Investigate existing service layer, exceptions, DTOs, and MapStruct mappers in `omnicare-emr-api` and design the business logic layer for Phase 2:
1. Review `PatientService`, `PractitionerService`, `GlobalExceptionHandler`, DTOs, Mappers, and exception classes.
2. Design DTOs and Mappers:
   - `EncounterRequestDto` (patientId, practitionerId, encounterDate, status, reason) with validation annotations (`@NotNull`).
   - `EncounterResponseDto` (id, patientId, patientName, practitionerId, practitionerName, encounterDate, status, reason, createdAt, updatedAt, version).
   - `ObservationRequestDto` (encounterId, valueJson / Map<String, Object> / JsonNode) with `@NotNull`.
   - `ObservationResponseDto` (id, encounterId, valueJson, createdAt, updatedAt, version).
   - MapStruct Mappers: `EncounterMapper`, `ObservationMapper`.
3. Design Service Layer & Clinical Business Rules:
   - `EncounterService` & `EncounterServiceImpl`: `createEncounter` (sets status to `PLANNED` by default if null or per request), `getEncounterById`, `getEncounters`.
   - `ObservationService` & `ObservationServiceImpl`: `createObservation`:
     * Validation 1: Check `Encounter` exists by ID. If not found, throw `ResourceNotFoundException` (HTTP 404).
     * Validation 2: Check `Encounter` status is NOT `CANCELLED`. If status is `CANCELLED`, throw `EncounterCancelledException` (or validation exception, returning HTTP 400 or 409).
     * Store JSON payload into `value_json` column.
     * `getObservationsByEncounterId(UUID encounterId)`.
4. Review `GlobalExceptionHandler` and design RFC 7807 response mapping for `EncounterCancelledException` / HTTP 400 or 409.

Write your findings and complete specification into `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_p2_2/handoff.md` and `progress.md`.
Send a message back when complete.
