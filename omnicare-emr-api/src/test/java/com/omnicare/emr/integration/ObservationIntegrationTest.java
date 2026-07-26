package com.omnicare.emr.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicare.emr.dto.ObservationRequestDto;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.entity.PractitionerType;
import com.omnicare.emr.repository.EncounterRepository;
import com.omnicare.emr.repository.ObservationRepository;
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
class ObservationIntegrationTest {

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
    private com.omnicare.emr.repository.DiagnosisRepository diagnosisRepository;

    @Autowired
    private com.omnicare.emr.repository.PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    private com.omnicare.emr.repository.DiagnosticReportRepository diagnosticReportRepository;

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
        diagnosticReportRepository.deleteAll();
        observationRepository.deleteAll();
        encounterRepository.deleteAll();
        patientRepository.deleteAll();
        practitionerRepository.deleteAll();

        savedPatient = patientRepository.save(Patient.builder()
                .identifier("079111222333")
                .fullName("Jane Vitals Patient")
                .gender("female")
                .birthDate(LocalDate.of(1988, 8, 8))
                .phoneNumber("+84911112222")
                .build());

        savedPractitioner = practitionerRepository.save(Practitioner.builder()
                .practitionerCode("PRAC-INT-02")
                .fullName("Dr. Vitals Examiner")
                .specialty("GENERAL_PRACTICE")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+84911113333")
                .email("vitals.doc@omnicare.com")
                .build());

        activeEncounter = encounterRepository.save(Encounter.builder()
                .patient(savedPatient)
                .practitioner(savedPractitioner)
                .encounterDate(Instant.now())
                .status(EncounterStatus.IN_PROGRESS)
                .reason("Active Examination")
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
    void testRecordObservation_PreservesJsonbVitalsPayload() throws Exception {
        JsonNode vitalsPayload = objectMapper.readTree("{\"bloodPressure\": \"120/80\", \"heartRate\": 75, \"temp\": 37.0}");

        ObservationRequestDto request = ObservationRequestDto.builder()
                .encounterId(activeEncounter.getId())
                .valueJson(vitalsPayload)
                .build();

        // 1. Post Vitals Observation
        String responseContent = mockMvc.perform(post("/api/v1/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.encounterId").value(activeEncounter.getId().toString()))
                .andExpect(jsonPath("$.valueJson.bloodPressure").value("120/80"))
                .andExpect(jsonPath("$.valueJson.heartRate").value(75))
                .andExpect(jsonPath("$.valueJson.temp").value(37.0))
                .andReturn().getResponse().getContentAsString();

        String observationId = objectMapper.readTree(responseContent).get("id").asText();

        // 2. Query Observations By Encounter ID
        mockMvc.perform(get("/api/v1/observations")
                .param("encounterId", activeEncounter.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(observationId))
                .andExpect(jsonPath("$[0].valueJson.bloodPressure").value("120/80"))
                .andExpect(jsonPath("$[0].valueJson.heartRate").value(75))
                .andExpect(jsonPath("$[0].valueJson.temp").value(37.0));
    }

    @Test
    void testRecordObservation_MissingEncounter_Returns404NotFound() throws Exception {
        UUID nonExistentEncounterId = UUID.randomUUID();
        JsonNode vitalsPayload = objectMapper.readTree("{\"heartRate\": 80}");

        ObservationRequestDto request = ObservationRequestDto.builder()
                .encounterId(nonExistentEncounterId)
                .valueJson(vitalsPayload)
                .build();

        mockMvc.perform(post("/api/v1/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testRecordObservation_CancelledEncounter_ReturnsRfc7807EncounterCancelledError() throws Exception {
        JsonNode vitalsPayload = objectMapper.readTree("{\"bloodPressure\": \"130/85\", \"heartRate\": 85}");

        ObservationRequestDto request = ObservationRequestDto.builder()
                .encounterId(cancelledEncounter.getId())
                .valueJson(vitalsPayload)
                .build();

        mockMvc.perform(post("/api/v1/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Encounter Cancelled"))
                .andExpect(jsonPath("$.type").value("https://api.omnicare.com/errors/encounter-cancelled"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Cannot record observation for cancelled encounter with ID: " + cancelledEncounter.getId()));
    }
}
