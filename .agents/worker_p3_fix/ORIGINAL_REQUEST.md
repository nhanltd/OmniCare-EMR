## 2026-07-25T09:11:03Z
You are the Remediation Worker for Phase 3 of OmniCare EMR.
Working directory for implementation: c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api
Your agent directory: c:/Users/nhan/Workspace/OmniCare-EMR/.agents/worker_p3_fix

Reviewer 2 and Challenger 2 identified two specific issues in `omnicare-emr-api` that must be fixed:

1. **Missing `@Valid` in `EncounterController.java`**:
   In `src/main/java/com/omnicare/emr/controller/EncounterController.java` (line 124), add `@Valid` before `@RequestBody FinalizeEncounterRequestDto request`:
   ```java
   @PostMapping("/{id}/finalize")
   @Operation(summary = "Finalize encounter", description = "Finalize encounter with diagnoses and prescriptions")
   public ResponseEntity<FinalizeEncounterResponseDto> finalizeEncounter(
           @PathVariable("id") UUID id,
           @Valid @RequestBody FinalizeEncounterRequestDto request) {
       return ResponseEntity.ok(encounterService.finalizeEncounter(id, request));
   }
   ```

2. **Missing OpenAPI Annotations in `DiagnosticReportController.java`**:
   In `src/main/java/com/omnicare/emr/controller/DiagnosticReportController.java`, add comprehensive OpenAPI 3 annotations (`@Tag(name = "Diagnostic Reports", description = "Diagnostic Report Management & LIS Webhook APIs")`, `@Operation`, `@ApiResponses`, `@Parameter`, `@Content`, `@Schema`) to all controller methods (`createDiagnosticReport`, `updateDiagnosticReportResults`, `getDiagnosticReportById`, `getDiagnosticReportsByEncounterId`).

3. **Verify Build & Tests**:
   Run `mvn clean compile` and `mvn test` in `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api`. Ensure all tests pass, including `Phase3EdgeCasesIntegrationTest`.

4. **Handoff Report**:
   Document changes and test results in `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/worker_p3_fix/handoff.md`.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
