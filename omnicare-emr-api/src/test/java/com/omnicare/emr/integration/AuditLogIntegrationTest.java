package com.omnicare.emr.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicare.emr.dto.DiagnosisRequestDto;
import com.omnicare.emr.dto.FinalizeEncounterRequestDto;
import com.omnicare.emr.dto.PrescriptionItemRequestDto;
import com.omnicare.emr.entity.AuditLog;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.entity.PractitionerType;
import com.omnicare.emr.repository.AuditLogRepository;
import com.omnicare.emr.repository.DiagnosisRepository;
import com.omnicare.emr.repository.EncounterRepository;
import com.omnicare.emr.repository.PatientRepository;
import com.omnicare.emr.repository.PractitionerRepository;
import com.omnicare.emr.repository.PrescriptionItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogIntegrationTest {

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
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private com.omnicare.emr.repository.DiagnosticReportRepository diagnosticReportRepository;

    @Autowired
    private com.omnicare.emr.repository.ObservationRepository observationRepository;

    private Patient savedPatient;
    private Practitioner savedPractitioner;
    private Encounter plannedEncounter;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        prescriptionItemRepository.deleteAll();
        diagnosisRepository.deleteAll();
        observationRepository.deleteAll();
        diagnosticReportRepository.deleteAll();
        encounterRepository.deleteAll();
        patientRepository.deleteAll();
        practitionerRepository.deleteAll();

        savedPatient = patientRepository.save(Patient.builder()
                .identifier("333444555666")
                .fullName("Audit Test Patient")
                .gender("female")
                .birthDate(LocalDate.of(1988, 7, 10))
                .phoneNumber("+84933333333")
                .build());

        savedPractitioner = practitionerRepository.save(Practitioner.builder()
                .practitionerCode("PRAC-AUD-01")
                .fullName("Dr. Audit Tester")
                .specialty("GENERAL")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+84933333334")
                .email("audit.tester@omnicare.com")
                .build());

        plannedEncounter = encounterRepository.save(Encounter.builder()
                .patient(savedPatient)
                .practitioner(savedPractitioner)
                .encounterDate(Instant.now())
                .status(EncounterStatus.PLANNED)
                .reason("Audited Consultation")
                .build());
    }

    @Test
    void testStatusTransition_TriggersAuditLogAutomatically() throws Exception {
        // 1. Transition PLANNED -> IN_PROGRESS via status update endpoint
        mockMvc.perform(put("/api/v1/encounters/{id}/status", plannedEncounter.getId())
                .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk());

        List<AuditLog> auditLogsAfterStatusChange = auditLogRepository.findByEntityId(plannedEncounter.getId());
        assertThat(auditLogsAfterStatusChange).hasSize(1);

        AuditLog log1 = auditLogsAfterStatusChange.get(0);
        assertThat(log1.getEntityId()).isEqualTo(plannedEncounter.getId());
        assertThat(log1.getOldStatus()).isEqualTo("PLANNED");
        assertThat(log1.getNewStatus()).isEqualTo("IN_PROGRESS");
        assertThat(log1.getAction()).isEqualTo("ENCOUNTER_STATUS_CHANGE");
        assertThat(log1.getChangedAt()).isNotNull();

        // 2. Transition IN_PROGRESS -> FINISHED via finalize endpoint
        FinalizeEncounterRequestDto finalizeRequest = FinalizeEncounterRequestDto.builder()
                .diagnoses(List.of(DiagnosisRequestDto.builder()
                        .icd10Code("K21.9")
                        .description("Gastro-esophageal reflux disease without esophagitis")
                        .build()))
                .prescriptions(List.of(PrescriptionItemRequestDto.builder()
                        .medicationName("Omeprazole")
                        .dosage(20.0)
                        .frequency("Once daily")
                        .duration("14 days")
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/encounters/{id}/finalize", plannedEncounter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(finalizeRequest)))
                .andExpect(status().isOk());

        List<AuditLog> auditLogsAfterFinalize = auditLogRepository.findByEntityIdOrderByChangedAtDesc(plannedEncounter.getId());
        assertThat(auditLogsAfterFinalize).hasSize(2);

        AuditLog log2 = auditLogsAfterFinalize.get(0);
        assertThat(log2.getEntityId()).isEqualTo(plannedEncounter.getId());
        assertThat(log2.getOldStatus()).isEqualTo("IN_PROGRESS");
        assertThat(log2.getNewStatus()).isEqualTo("FINISHED");
        assertThat(log2.getAction()).isEqualTo("ENCOUNTER_STATUS_CHANGE");
        assertThat(log2.getChangedAt()).isNotNull();
    }
}
