package com.omnicare.emr.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicare.emr.dto.EncounterRequestDto;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.entity.PractitionerType;
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
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EncounterIntegrationTest {

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
    private com.omnicare.emr.repository.DiagnosisRepository diagnosisRepository;

    @Autowired
    private com.omnicare.emr.repository.PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private com.omnicare.emr.repository.ObservationRepository observationRepository;

    @Autowired
    private com.omnicare.emr.repository.DiagnosticReportRepository diagnosticReportRepository;

    @Autowired
    private com.omnicare.emr.repository.AuditLogRepository auditLogRepository;

    private Patient savedPatient;
    private Practitioner savedPractitioner;

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

        Patient patient = Patient.builder()
                .identifier("079999888777")
                .fullName("Integration Test Patient")
                .gender("male")
                .birthDate(LocalDate.of(1990, 1, 1))
                .phoneNumber("+84900000000")
                .build();
        savedPatient = patientRepository.save(patient);

        Practitioner practitioner = Practitioner.builder()
                .practitionerCode("PRAC-INT-01")
                .fullName("Dr. Integration Tester")
                .specialty("CARDIOLOGY")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+84900000001")
                .email("int.tester@omnicare.com")
                .build();
        savedPractitioner = practitionerRepository.save(practitioner);
    }

    @Test
    void testEncounterLifecycle_CreateGetList() throws Exception {
        EncounterRequestDto request = EncounterRequestDto.builder()
                .patientId(savedPatient.getId())
                .practitionerId(savedPractitioner.getId())
                .encounterDate(Instant.now())
                .reason("Initial Integration Consultation")
                .build();

        // 1. Create Encounter -> Status should default to PLANNED
        String createResponseBody = mockMvc.perform(post("/api/v1/encounters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.patientId").value(savedPatient.getId().toString()))
                .andExpect(jsonPath("$.patientName").value("Integration Test Patient"))
                .andExpect(jsonPath("$.practitionerId").value(savedPractitioner.getId().toString()))
                .andExpect(jsonPath("$.practitionerName").value("Dr. Integration Tester"))
                .andReturn().getResponse().getContentAsString();

        String createdId = objectMapper.readTree(createResponseBody).get("id").asText();

        // 2. Get Encounter By ID
        mockMvc.perform(get("/api/v1/encounters/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.reason").value("Initial Integration Consultation"));

        // 3. List All Encounters
        mockMvc.perform(get("/api/v1/encounters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(createdId));
    }

    @Test
    void testCreateEncounter_InvalidPatientId_Returns404() throws Exception {
        EncounterRequestDto request = EncounterRequestDto.builder()
                .patientId(UUID.randomUUID())
                .practitionerId(savedPractitioner.getId())
                .encounterDate(Instant.now())
                .reason("Consultation with non-existent patient")
                .build();

        mockMvc.perform(post("/api/v1/encounters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
