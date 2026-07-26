package com.omnicare.emr.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicare.emr.dto.DiagnosticReportResultUpdateDto;
import com.omnicare.emr.entity.DiagnosticReport;
import com.omnicare.emr.entity.DiagnosticReportStatus;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.entity.PractitionerType;
import com.omnicare.emr.repository.DiagnosticReportRepository;
import com.omnicare.emr.repository.EncounterRepository;
import com.omnicare.emr.repository.PatientRepository;
import com.omnicare.emr.repository.PractitionerRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiagnosticReportIntegrationTest {

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
    private com.omnicare.emr.repository.DiagnosisRepository diagnosisRepository;

    @Autowired
    private com.omnicare.emr.repository.PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private com.omnicare.emr.repository.ObservationRepository observationRepository;

    @Autowired
    private com.omnicare.emr.repository.AuditLogRepository auditLogRepository;

    private Patient savedPatient;
    private Practitioner savedPractitioner;
    private Encounter activeEncounter;
    private Encounter cancelledEncounter;

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
                .identifier("111222333444")
                .fullName("Diagnostic Test Patient")
                .gender("female")
                .birthDate(LocalDate.of(1985, 5, 15))
                .phoneNumber("+84911111111")
                .build());

        savedPractitioner = practitionerRepository.save(Practitioner.builder()
                .practitionerCode("PRAC-DIAG-01")
                .fullName("Dr. Lab Specialist")
                .specialty("PATHOLOGY")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+84911111112")
                .email("lab.spec@omnicare.com")
                .build());

        activeEncounter = encounterRepository.save(Encounter.builder()
                .patient(savedPatient)
                .practitioner(savedPractitioner)
                .encounterDate(Instant.now())
                .status(EncounterStatus.IN_PROGRESS)
                .reason("Blood Work & Urinalysis")
                .build());

        cancelledEncounter = encounterRepository.save(Encounter.builder()
                .patient(savedPatient)
                .practitioner(savedPractitioner)
                .encounterDate(Instant.now())
                .status(EncounterStatus.CANCELLED)
                .reason("Cancelled Consultation")
                .build());
    }

    @Test
    void testUpdateDiagnosticReportResults_Success() throws Exception {
        DiagnosticReport report = diagnosticReportRepository.save(DiagnosticReport.builder()
                .encounter(activeEncounter)
                .testCode("GLU-001")
                .testName("Fasting Plasma Glucose")
                .orderedAt(Instant.now())
                .status(DiagnosticReportStatus.ORDERED)
                .build());

        DiagnosticReportResultUpdateDto updateDto = DiagnosticReportResultUpdateDto.builder()
                .resultValue("105 mg/dL")
                .unit("mg/dL")
                .referenceRange("70-99 mg/dL")
                .flag("HIGH")
                .status(DiagnosticReportStatus.FINAL)
                .build();

        mockMvc.perform(put("/api/v1/diagnostic-reports/{id}/results", report.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(report.getId().toString()))
                .andExpect(jsonPath("$.resultValue").value("105 mg/dL"))
                .andExpect(jsonPath("$.unit").value("mg/dL"))
                .andExpect(jsonPath("$.referenceRange").value("70-99 mg/dL"))
                .andExpect(jsonPath("$.flag").value("HIGH"))
                .andExpect(jsonPath("$.status").value("FINAL"))
                .andExpect(jsonPath("$.resultReceivedAt").exists());

        DiagnosticReport updatedReport = diagnosticReportRepository.findById(report.getId()).orElseThrow();
        assertThat(updatedReport.getResultValue()).isEqualTo("105 mg/dL");
        assertThat(updatedReport.getResultReceivedAt()).isNotNull();
        assertThat(updatedReport.getStatus()).isEqualTo(DiagnosticReportStatus.FINAL);
    }

    @Test
    void testUpdateDiagnosticReportResults_CancelledEncounter_Returns400() throws Exception {
        DiagnosticReport reportOnCancelledEncounter = diagnosticReportRepository.save(DiagnosticReport.builder()
                .encounter(cancelledEncounter)
                .testCode("CBC-001")
                .testName("Complete Blood Count")
                .orderedAt(Instant.now())
                .status(DiagnosticReportStatus.ORDERED)
                .build());

        DiagnosticReportResultUpdateDto updateDto = DiagnosticReportResultUpdateDto.builder()
                .resultValue("14.2 g/dL")
                .unit("g/dL")
                .status(DiagnosticReportStatus.FINAL)
                .build();

        mockMvc.perform(put("/api/v1/diagnostic-reports/{id}/results", reportOnCancelledEncounter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Encounter Cancelled"));
    }
}
