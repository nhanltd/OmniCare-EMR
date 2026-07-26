package com.omnicare.emr.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicare.emr.dto.DiagnosisRequestDto;
import com.omnicare.emr.dto.DiagnosticReportResultUpdateDto;
import com.omnicare.emr.dto.FinalizeEncounterRequestDto;
import com.omnicare.emr.dto.PrescriptionItemRequestDto;
import com.omnicare.emr.entity.AuditLog;
import com.omnicare.emr.entity.Diagnosis;
import com.omnicare.emr.entity.DiagnosticReport;
import com.omnicare.emr.entity.DiagnosticReportStatus;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.entity.PractitionerType;
import com.omnicare.emr.entity.PrescriptionItem;
import com.omnicare.emr.repository.AuditLogRepository;
import com.omnicare.emr.repository.DiagnosisRepository;
import com.omnicare.emr.repository.DiagnosticReportRepository;
import com.omnicare.emr.repository.EncounterRepository;
import com.omnicare.emr.repository.PatientRepository;
import com.omnicare.emr.repository.PractitionerRepository;
import com.omnicare.emr.repository.PrescriptionItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase3EdgeCasesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PractitionerRepository practitionerRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private DiagnosticReportRepository diagnosticReportRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Patient testPatient;
    private Practitioner testPractitioner;
    private Encounter plannedEncounter;
    private Encounter inProgressEncounter;
    private Encounter cancelledEncounter;
    private Encounter finishedEncounter;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        prescriptionItemRepository.deleteAll();
        diagnosisRepository.deleteAll();
        diagnosticReportRepository.deleteAll();
        encounterRepository.deleteAll();
        patientRepository.deleteAll();
        practitionerRepository.deleteAll();

        testPatient = patientRepository.save(Patient.builder()
                .identifier("999888777666")
                .fullName("Phase 3 Challenger Patient")
                .gender("other")
                .birthDate(LocalDate.of(1990, 1, 1))
                .phoneNumber("+84999888777")
                .build());

        testPractitioner = practitionerRepository.save(Practitioner.builder()
                .practitionerCode("PRAC-CHALLENGE-01")
                .fullName("Dr. Challenger Specialist")
                .specialty("CRITICAL_CARE")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+84999888778")
                .email("challenger@omnicare.com")
                .build());

        plannedEncounter = encounterRepository.save(Encounter.builder()
                .patient(testPatient)
                .practitioner(testPractitioner)
                .encounterDate(Instant.now())
                .status(EncounterStatus.PLANNED)
                .reason("Planned ICU Check")
                .build());

        inProgressEncounter = encounterRepository.save(Encounter.builder()
                .patient(testPatient)
                .practitioner(testPractitioner)
                .encounterDate(Instant.now())
                .status(EncounterStatus.IN_PROGRESS)
                .reason("In-Progress Treatment")
                .build());

        cancelledEncounter = encounterRepository.save(Encounter.builder()
                .patient(testPatient)
                .practitioner(testPractitioner)
                .encounterDate(Instant.now())
                .status(EncounterStatus.CANCELLED)
                .reason("Cancelled Patient Request")
                .build());

        finishedEncounter = encounterRepository.save(Encounter.builder()
                .patient(testPatient)
                .practitioner(testPractitioner)
                .encounterDate(Instant.now())
                .status(EncounterStatus.FINISHED)
                .reason("Completed Visit")
                .build());
    }

    // ==========================================
    // 1. LIS Webhook Edge Cases
    // ==========================================

    @Test
    @DisplayName("LIS Webhook - Missing optional fields (unit, referenceRange, flag null/omitted)")
    void testLisWebhook_MissingOptionalFields_Success() throws Exception {
        DiagnosticReport report = diagnosticReportRepository.save(DiagnosticReport.builder()
                .encounter(inProgressEncounter)
                .testCode("HEMO-01")
                .testName("Hemoglobin")
                .orderedAt(Instant.now())
                .status(DiagnosticReportStatus.ORDERED)
                .build());

        DiagnosticReportResultUpdateDto updateDto = DiagnosticReportResultUpdateDto.builder()
                .resultValue("13.5")
                .status(DiagnosticReportStatus.FINAL)
                .build();

        mockMvc.perform(put("/api/v1/diagnostic-reports/{id}/results", report.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(report.getId().toString()))
                .andExpect(jsonPath("$.resultValue").value("13.5"))
                .andExpect(jsonPath("$.status").value("FINAL"))
                .andExpect(jsonPath("$.unit").doesNotExist())
                .andExpect(jsonPath("$.referenceRange").doesNotExist())
                .andExpect(jsonPath("$.flag").doesNotExist())
                .andExpect(jsonPath("$.resultReceivedAt").exists());

        DiagnosticReport updatedReport = diagnosticReportRepository.findById(report.getId()).orElseThrow();
        assertThat(updatedReport.getResultValue()).isEqualTo("13.5");
        assertThat(updatedReport.getUnit()).isNull();
        assertThat(updatedReport.getReferenceRange()).isNull();
        assertThat(updatedReport.getFlag()).isNull();
        assertThat(updatedReport.getStatus()).isEqualTo(DiagnosticReportStatus.FINAL);
        assertThat(updatedReport.getResultReceivedAt()).isNotNull();
    }

    @Test
    @DisplayName("LIS Webhook - Updating an already FINAL report")
    void testLisWebhook_UpdatingAlreadyFinalReport_UpdatesTimestampAndValue() throws Exception {
        Instant initialResultTime = Instant.now().minusSeconds(3600);
        DiagnosticReport report = diagnosticReportRepository.save(DiagnosticReport.builder()
                .encounter(inProgressEncounter)
                .testCode("PLT-01")
                .testName("Platelet Count")
                .orderedAt(Instant.now().minusSeconds(7200))
                .resultReceivedAt(initialResultTime)
                .resultValue("250")
                .unit("10^9/L")
                .status(DiagnosticReportStatus.FINAL)
                .build());

        DiagnosticReportResultUpdateDto updateDto = DiagnosticReportResultUpdateDto.builder()
                .resultValue("255")
                .unit("10^9/L")
                .referenceRange("150-450")
                .flag("NORMAL")
                .status(DiagnosticReportStatus.FINAL)
                .build();

        mockMvc.perform(put("/api/v1/diagnostic-reports/{id}/results", report.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultValue").value("255"))
                .andExpect(jsonPath("$.flag").value("NORMAL"));

        DiagnosticReport updatedReport = diagnosticReportRepository.findById(report.getId()).orElseThrow();
        assertThat(updatedReport.getResultValue()).isEqualTo("255");
        assertThat(updatedReport.getResultReceivedAt()).isAfterOrEqualTo(initialResultTime);
    }

    @Test
    @DisplayName("LIS Webhook - Rejection when Encounter is CANCELLED")
    void testLisWebhook_CancelledEncounter_Returns400() throws Exception {
        DiagnosticReport reportOnCancelled = diagnosticReportRepository.save(DiagnosticReport.builder()
                .encounter(cancelledEncounter)
                .testCode("URINE-01")
                .testName("Urinalysis")
                .orderedAt(Instant.now())
                .status(DiagnosticReportStatus.ORDERED)
                .build());

        DiagnosticReportResultUpdateDto updateDto = DiagnosticReportResultUpdateDto.builder()
                .resultValue("Clear")
                .status(DiagnosticReportStatus.FINAL)
                .build();

        mockMvc.perform(put("/api/v1/diagnostic-reports/{id}/results", reportOnCancelled.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Encounter Cancelled"));
    }

    // ==========================================
    // 2. Transactional Finalize Edge Cases
    // ==========================================

    @Test
    @DisplayName("Finalize Encounter - Zero dosage prescription item throws 400")
    void testFinalizeEncounter_ZeroDosage_Throws400() throws Exception {
        DiagnosisRequestDto diagnosis = DiagnosisRequestDto.builder()
                .icd10Code("J18.9")
                .description("Pneumonia, unspecified organism")
                .build();

        PrescriptionItemRequestDto zeroDosageItem = PrescriptionItemRequestDto.builder()
                .medicationName("Amoxicillin")
                .dosage(0.0)
                .frequency("3 times daily")
                .duration("7 days")
                .build();

        FinalizeEncounterRequestDto request = FinalizeEncounterRequestDto.builder()
                .diagnoses(List.of(diagnosis))
                .prescriptions(List.of(zeroDosageItem))
                .build();

        mockMvc.perform(post("/api/v1/encounters/{id}/finalize", inProgressEncounter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Finalize Encounter - Negative dosage prescription item throws 400")
    void testFinalizeEncounter_NegativeDosage_Throws400() throws Exception {
        DiagnosisRequestDto diagnosis = DiagnosisRequestDto.builder()
                .icd10Code("I10")
                .description("Essential hypertension")
                .build();

        PrescriptionItemRequestDto negativeDosageItem = PrescriptionItemRequestDto.builder()
                .medicationName("Lisinopril")
                .dosage(-10.0)
                .frequency("Once daily")
                .duration("30 days")
                .build();

        FinalizeEncounterRequestDto request = FinalizeEncounterRequestDto.builder()
                .diagnoses(List.of(diagnosis))
                .prescriptions(List.of(negativeDosageItem))
                .build();

        mockMvc.perform(post("/api/v1/encounters/{id}/finalize", inProgressEncounter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Finalize Encounter - Already FINISHED encounter throws 400 Invalid State")
    void testFinalizeEncounter_AlreadyFinished_Throws400() throws Exception {
        DiagnosisRequestDto diagnosis = DiagnosisRequestDto.builder()
                .icd10Code("E11")
                .description("Type 2 diabetes")
                .build();

        PrescriptionItemRequestDto prescription = PrescriptionItemRequestDto.builder()
                .medicationName("Metformin")
                .dosage(500.0)
                .frequency("2 times daily")
                .duration("30 days")
                .build();

        FinalizeEncounterRequestDto request = FinalizeEncounterRequestDto.builder()
                .diagnoses(List.of(diagnosis))
                .prescriptions(List.of(prescription))
                .build();

        mockMvc.perform(post("/api/v1/encounters/{id}/finalize", finishedEncounter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid State"))
                .andExpect(jsonPath("$.detail").value("Encounter is already finalized"));
    }

    @Test
    @DisplayName("Finalize Encounter - CANCELLED encounter throws 400 Encounter Cancelled")
    void testFinalizeEncounter_CancelledEncounter_Throws400() throws Exception {
        DiagnosisRequestDto diagnosis = DiagnosisRequestDto.builder()
                .icd10Code("Z00.0")
                .description("General medical examination")
                .build();

        PrescriptionItemRequestDto prescription = PrescriptionItemRequestDto.builder()
                .medicationName("Vitamin D3")
                .dosage(1000.0)
                .frequency("Once daily")
                .duration("60 days")
                .build();

        FinalizeEncounterRequestDto request = FinalizeEncounterRequestDto.builder()
                .diagnoses(List.of(diagnosis))
                .prescriptions(List.of(prescription))
                .build();

        mockMvc.perform(post("/api/v1/encounters/{id}/finalize", cancelledEncounter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Encounter Cancelled"))
                .andExpect(jsonPath("$.detail").value("Cannot finalize a cancelled encounter"));
    }

    @Test
    @DisplayName("Finalize Encounter - Empty diagnoses list fails @Valid validation with 400")
    void testFinalizeEncounter_EmptyDiagnosesList_FailsValidationWhenValidAnnotationPresent() throws Exception {
        PrescriptionItemRequestDto prescription = PrescriptionItemRequestDto.builder()
                .medicationName("Aspirin")
                .dosage(81.0)
                .frequency("Once daily")
                .duration("30 days")
                .build();

        // Empty diagnosis list
        FinalizeEncounterRequestDto request = FinalizeEncounterRequestDto.builder()
                .diagnoses(Collections.emptyList())
                .prescriptions(List.of(prescription))
                .build();

        // With @Valid present on EncounterController.finalizeEncounter(@Valid @RequestBody FinalizeEncounterRequestDto),
        // Spring MVC triggers @NotEmpty validation on the DTO and returns 400 Bad Request.
        mockMvc.perform(post("/api/v1/encounters/{id}/finalize", plannedEncounter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 3. Zero-Partial-Writes Rollback Verification
    // ==========================================

    @Test
    @DisplayName("Zero-Partial-Writes - Multiple valid diagnoses rolled back when prescription validation fails")
    void testZeroPartialWrites_RollbackGuaranteed() throws Exception {
        DiagnosisRequestDto d1 = DiagnosisRequestDto.builder().icd10Code("A00.0").description("Cholera").build();
        DiagnosisRequestDto d2 = DiagnosisRequestDto.builder().icd10Code("A01.0").description("Typhoid fever").build();
        DiagnosisRequestDto d3 = DiagnosisRequestDto.builder().icd10Code("A02.0").description("Salmonella enteritis").build();

        PrescriptionItemRequestDto validRx = PrescriptionItemRequestDto.builder()
                .medicationName("Ciprofloxacin")
                .dosage(500.0)
                .frequency("Twice daily")
                .duration("7 days")
                .build();

        PrescriptionItemRequestDto invalidRx = PrescriptionItemRequestDto.builder()
                .medicationName("Oral Rehydration Solution")
                .dosage(-1.0)
                .frequency("As needed")
                .duration("3 days")
                .build();

        FinalizeEncounterRequestDto request = FinalizeEncounterRequestDto.builder()
                .diagnoses(List.of(d1, d2, d3))
                .prescriptions(List.of(validRx, invalidRx))
                .build();

        mockMvc.perform(post("/api/v1/encounters/{id}/finalize", plannedEncounter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // EMPIRICAL VERIFICATION: Check database directly
        List<Diagnosis> savedDiagnoses = diagnosisRepository.findByEncounterIdAndIsDeletedFalse(plannedEncounter.getId());
        assertThat(savedDiagnoses).isEmpty();

        List<PrescriptionItem> savedPrescriptions = prescriptionItemRepository.findByEncounterIdAndIsDeletedFalse(plannedEncounter.getId());
        assertThat(savedPrescriptions).isEmpty();

        Encounter reloadedEncounter = encounterRepository.findById(plannedEncounter.getId()).orElseThrow();
        assertThat(reloadedEncounter.getStatus()).isEqualTo(EncounterStatus.PLANNED);
    }

    // ==========================================
    // 4. Spring AOP Audit Trail Verification
    // ==========================================

    @Test
    @DisplayName("Audit Trail - Sequence of multiple status transitions logged precisely")
    void testAuditTrail_MultipleTransitionsSequence() throws Exception {
        // Step 1: Transition PLANNED -> IN_PROGRESS
        mockMvc.perform(put("/api/v1/encounters/{id}/status", plannedEncounter.getId())
                .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk());

        // Step 2: Finalize encounter IN_PROGRESS -> FINISHED
        FinalizeEncounterRequestDto finalizeRequest = FinalizeEncounterRequestDto.builder()
                .diagnoses(List.of(DiagnosisRequestDto.builder().icd10Code("J44.9").description("COPD, unspecified").build()))
                .prescriptions(List.of(PrescriptionItemRequestDto.builder().medicationName("Salbutamol").dosage(100.0).frequency("2 puffs QID").duration("30 days").build()))
                .build();

        mockMvc.perform(post("/api/v1/encounters/{id}/finalize", plannedEncounter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(finalizeRequest)))
                .andExpect(status().isOk());

        // Verify audit logs in order
        List<AuditLog> auditLogs = auditLogRepository.findByEntityId(plannedEncounter.getId());
        assertThat(auditLogs).hasSize(2);

        AuditLog firstTransition = auditLogs.stream()
                .filter(log -> "PLANNED".equals(log.getOldStatus()) && "IN_PROGRESS".equals(log.getNewStatus()))
                .findFirst()
                .orElseThrow();
        assertThat(firstTransition.getAction()).isEqualTo("ENCOUNTER_STATUS_CHANGE");
        assertThat(firstTransition.getChangedAt()).isNotNull();

        AuditLog secondTransition = auditLogs.stream()
                .filter(log -> "IN_PROGRESS".equals(log.getOldStatus()) && "FINISHED".equals(log.getNewStatus()))
                .findFirst()
                .orElseThrow();
        assertThat(secondTransition.getAction()).isEqualTo("ENCOUNTER_STATUS_CHANGE");
        assertThat(secondTransition.getChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("Audit Trail - No audit log created on failed transition attempt")
    void testAuditTrail_FailedAttempt_NoAuditLogCreated() throws Exception {
        int initialLogCount = auditLogRepository.findByEntityId(cancelledEncounter.getId()).size();

        FinalizeEncounterRequestDto finalizeRequest = FinalizeEncounterRequestDto.builder()
                .diagnoses(List.of(DiagnosisRequestDto.builder().icd10Code("R05").description("Cough").build()))
                .prescriptions(List.of(PrescriptionItemRequestDto.builder().medicationName("Cough Syrup").dosage(10.0).frequency("TID").duration("5 days").build()))
                .build();

        mockMvc.perform(post("/api/v1/encounters/{id}/finalize", cancelledEncounter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(finalizeRequest)))
                .andExpect(status().isBadRequest());

        List<AuditLog> auditLogsAfterFailure = auditLogRepository.findByEntityId(cancelledEncounter.getId());
        assertThat(auditLogsAfterFailure).hasSize(initialLogCount);
    }

    @Test
    @DisplayName("Audit Trail - Updating encounter to same status does not duplicate audit log")
    void testAuditTrail_SameStatusUpdate_NoDuplicateLog() throws Exception {
        // Initial transition PLANNED -> IN_PROGRESS
        mockMvc.perform(put("/api/v1/encounters/{id}/status", plannedEncounter.getId())
                .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk());

        int countAfterFirstChange = auditLogRepository.findByEntityId(plannedEncounter.getId()).size();
        assertThat(countAfterFirstChange).isEqualTo(1);

        // Second transition IN_PROGRESS -> IN_PROGRESS (no change)
        mockMvc.perform(put("/api/v1/encounters/{id}/status", plannedEncounter.getId())
                .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk());

        int countAfterNoChange = auditLogRepository.findByEntityId(plannedEncounter.getId()).size();
        assertThat(countAfterNoChange).isEqualTo(1);
    }
}
