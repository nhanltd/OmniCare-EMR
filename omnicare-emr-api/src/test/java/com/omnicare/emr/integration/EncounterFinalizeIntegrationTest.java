package com.omnicare.emr.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicare.emr.dto.DiagnosisRequestDto;
import com.omnicare.emr.dto.FinalizeEncounterRequestDto;
import com.omnicare.emr.dto.PrescriptionItemRequestDto;
import com.omnicare.emr.entity.Diagnosis;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.entity.PractitionerType;
import com.omnicare.emr.entity.PrescriptionItem;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EncounterFinalizeIntegrationTest {

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
    private com.omnicare.emr.repository.DiagnosticReportRepository diagnosticReportRepository;

    @Autowired
    private com.omnicare.emr.repository.ObservationRepository observationRepository;

    @Autowired
    private com.omnicare.emr.repository.AuditLogRepository auditLogRepository;

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
                .identifier("222333444555")
                .fullName("Finalize Test Patient")
                .gender("male")
                .birthDate(LocalDate.of(1992, 3, 20))
                .phoneNumber("+84922222222")
                .build());

        savedPractitioner = practitionerRepository.save(Practitioner.builder()
                .practitionerCode("PRAC-FIN-01")
                .fullName("Dr. Attending Physician")
                .specialty("INTERNAL_MEDICINE")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+84922222223")
                .email("attending@omnicare.com")
                .build());

        plannedEncounter = encounterRepository.save(Encounter.builder()
                .patient(savedPatient)
                .practitioner(savedPractitioner)
                .encounterDate(Instant.now())
                .status(EncounterStatus.PLANNED)
                .reason("Routine Health Checkup")
                .build());
    }

    @Test
    void testFinalizeEncounter_Success() throws Exception {
        DiagnosisRequestDto diagnosis1 = DiagnosisRequestDto.builder()
                .icd10Code("E11.9")
                .description("Type 2 diabetes mellitus without complications")
                .build();

        DiagnosisRequestDto diagnosis2 = DiagnosisRequestDto.builder()
                .icd10Code("I10")
                .description("Essential (primary) hypertension")
                .build();

        PrescriptionItemRequestDto prescription1 = PrescriptionItemRequestDto.builder()
                .medicationName("Metformin")
                .dosage(500.0)
                .frequency("Twice daily")
                .duration("30 days")
                .build();

        PrescriptionItemRequestDto prescription2 = PrescriptionItemRequestDto.builder()
                .medicationName("Amlodipine")
                .dosage(5.0)
                .frequency("Once daily")
                .duration("30 days")
                .build();

        FinalizeEncounterRequestDto request = FinalizeEncounterRequestDto.builder()
                .diagnoses(List.of(diagnosis1, diagnosis2))
                .prescriptions(List.of(prescription1, prescription2))
                .build();

        mockMvc.perform(post("/api/v1/encounters/{id}/finalize", plannedEncounter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encounterId").value(plannedEncounter.getId().toString()))
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.diagnoses", hasSize(2)))
                .andExpect(jsonPath("$.prescriptions", hasSize(2)));

        Encounter updatedEncounter = encounterRepository.findById(plannedEncounter.getId()).orElseThrow();
        assertThat(updatedEncounter.getStatus()).isEqualTo(EncounterStatus.FINISHED);

        List<Diagnosis> savedDiagnoses = diagnosisRepository.findByEncounterIdAndIsDeletedFalse(plannedEncounter.getId());
        assertThat(savedDiagnoses).hasSize(2);

        List<PrescriptionItem> savedPrescriptions = prescriptionItemRepository.findByEncounterIdAndIsDeletedFalse(plannedEncounter.getId());
        assertThat(savedPrescriptions).hasSize(2);
    }

    @Test
    void testFinalizeEncounter_InvalidPrescriptionDosage_RollsBackDiagnoses() throws Exception {
        DiagnosisRequestDto diagnosis = DiagnosisRequestDto.builder()
                .icd10Code("J00")
                .description("Acute nasopharyngitis [common cold]")
                .build();

        // Invalid prescription item: dosage <= 0 (-5.0)
        PrescriptionItemRequestDto invalidPrescription = PrescriptionItemRequestDto.builder()
                .medicationName("Paracetamol")
                .dosage(-5.0)
                .frequency("3 times daily")
                .duration("5 days")
                .build();

        FinalizeEncounterRequestDto request = FinalizeEncounterRequestDto.builder()
                .diagnoses(List.of(diagnosis))
                .prescriptions(List.of(invalidPrescription))
                .build();

        mockMvc.perform(post("/api/v1/encounters/{id}/finalize", plannedEncounter.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify total transaction rollback: 0 diagnoses saved in DB
        List<Diagnosis> savedDiagnoses = diagnosisRepository.findByEncounterIdAndIsDeletedFalse(plannedEncounter.getId());
        assertThat(savedDiagnoses).isEmpty();

        // Verify 0 prescription items saved in DB
        List<PrescriptionItem> savedPrescriptions = prescriptionItemRepository.findByEncounterIdAndIsDeletedFalse(plannedEncounter.getId());
        assertThat(savedPrescriptions).isEmpty();

        // Verify encounter status remains PLANNED
        Encounter reloadedEncounter = encounterRepository.findById(plannedEncounter.getId()).orElseThrow();
        assertThat(reloadedEncounter.getStatus()).isEqualTo(EncounterStatus.PLANNED);
    }
}
