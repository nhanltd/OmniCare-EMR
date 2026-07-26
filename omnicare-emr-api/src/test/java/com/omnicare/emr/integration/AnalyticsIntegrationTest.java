package com.omnicare.emr.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicare.emr.entity.Diagnosis;
import com.omnicare.emr.entity.DiagnosticReport;
import com.omnicare.emr.entity.DiagnosticReportStatus;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Observation;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.entity.PractitionerType;
import com.omnicare.emr.entity.PrescriptionItem;
import com.omnicare.emr.repository.AuditLogRepository;
import com.omnicare.emr.repository.DiagnosisRepository;
import com.omnicare.emr.repository.DiagnosticReportRepository;
import com.omnicare.emr.repository.EncounterRepository;
import com.omnicare.emr.repository.ObservationRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsIntegrationTest {

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
    private ObservationRepository observationRepository;

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

        testPatient = patientRepository.save(Patient.builder()
                .identifier("079988776655")
                .fullName("Analytics Test Patient")
                .gender("male")
                .birthDate(LocalDate.of(1985, 4, 12))
                .phoneNumber("+84900112233")
                .build());

        testPractitioner = practitionerRepository.save(Practitioner.builder()
                .practitionerCode("PRAC-ANALYTICS-01")
                .fullName("Dr. Analytics Specialist")
                .specialty("INTERNAL_MEDICINE")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+84900112234")
                .email("analytics.doc@omnicare.com")
                .build());
    }

    @Test
    void testGetOperationalKpis_Success() throws Exception {
        Encounter encounter1 = encounterRepository.save(Encounter.builder()
                .patient(testPatient)
                .practitioner(testPractitioner)
                .encounterDate(Instant.now().minus(2, ChronoUnit.DAYS))
                .status(EncounterStatus.FINISHED)
                .reason("Routine Checkup")
                .build());

        Encounter encounter2 = encounterRepository.save(Encounter.builder()
                .patient(testPatient)
                .practitioner(testPractitioner)
                .encounterDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .status(EncounterStatus.IN_PROGRESS)
                .reason("Follow-up Examination")
                .build());

        Instant now = Instant.now();
        diagnosticReportRepository.save(DiagnosticReport.builder()
                .encounter(encounter1)
                .testCode("WBC-01")
                .testName("White Blood Cell Count")
                .orderedAt(now.minus(60, ChronoUnit.MINUTES))
                .resultReceivedAt(now)
                .status(DiagnosticReportStatus.FINAL)
                .build());

        diagnosisRepository.save(Diagnosis.builder()
                .encounter(encounter1)
                .icd10Code("E11.9")
                .description("Type 2 diabetes mellitus")
                .build());

        diagnosisRepository.save(Diagnosis.builder()
                .encounter(encounter2)
                .icd10Code("E11.9")
                .description("Type 2 diabetes mellitus")
                .build());

        mockMvc.perform(get("/api/v1/analytics/operational-kpis")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatients").value(1))
                .andExpect(jsonPath("$.totalPractitioners").value(1))
                .andExpect(jsonPath("$.totalEncounters").value(2))
                .andExpect(jsonPath("$.avgTurnaroundTimeMinutes").value(60.0))
                .andExpect(jsonPath("$.encounterStatusCounts.FINISHED").value(1))
                .andExpect(jsonPath("$.encounterStatusCounts.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.topIcd10Diagnoses['E11.9']").value(2));
    }

    @Test
    void testGetPatientClinicalHistory_Success() throws Exception {
        Encounter encounter = encounterRepository.save(Encounter.builder()
                .patient(testPatient)
                .practitioner(testPractitioner)
                .encounterDate(Instant.now())
                .status(EncounterStatus.FINISHED)
                .reason("Hypertension Evaluation")
                .build());

        JsonNode vitals = objectMapper.readTree("{\"bloodPressure\": \"135/85\", \"heartRate\": 78}");
        observationRepository.save(Observation.builder()
                .encounter(encounter)
                .valueJson(vitals)
                .build());

        diagnosticReportRepository.save(DiagnosticReport.builder()
                .encounter(encounter)
                .testCode("LIPID-01")
                .testName("Lipid Panel")
                .orderedAt(Instant.now().minus(30, ChronoUnit.MINUTES))
                .resultReceivedAt(Instant.now())
                .status(DiagnosticReportStatus.FINAL)
                .build());

        diagnosisRepository.save(Diagnosis.builder()
                .encounter(encounter)
                .icd10Code("I10")
                .description("Essential hypertension")
                .build());

        prescriptionItemRepository.save(PrescriptionItem.builder()
                .encounter(encounter)
                .medicationName("Lisinopril")
                .dosage(10.0)
                .frequency("Once daily")
                .duration("30 days")
                .build());

        mockMvc.perform(get("/api/v1/patients/{id}/clinical-history", testPatient.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient.id").value(testPatient.getId().toString()))
                .andExpect(jsonPath("$.patient.fullName").value("Analytics Test Patient"))
                .andExpect(jsonPath("$.totalEncounters").value(1))
                .andExpect(jsonPath("$.encounters", hasSize(1)))
                .andExpect(jsonPath("$.encounters[0].observations", hasSize(1)))
                .andExpect(jsonPath("$.encounters[0].diagnosticReports", hasSize(1)))
                .andExpect(jsonPath("$.encounters[0].diagnoses", hasSize(1)))
                .andExpect(jsonPath("$.encounters[0].prescriptions", hasSize(1)));
    }

    @Test
    void testGetPatientClinicalHistory_PatientNotFound_Returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();


        mockMvc.perform(get("/api/v1/patients/{id}/clinical-history", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
