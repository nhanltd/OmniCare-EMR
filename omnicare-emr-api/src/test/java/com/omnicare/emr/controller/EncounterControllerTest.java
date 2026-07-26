package com.omnicare.emr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicare.emr.dto.EncounterRequestDto;
import com.omnicare.emr.dto.EncounterResponseDto;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.exception.GlobalExceptionHandler;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.service.EncounterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EncounterController.class)
@Import(GlobalExceptionHandler.class)
class EncounterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EncounterService encounterService;

    @Test
    void createEncounter_Returns201Created() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID practitionerId = UUID.randomUUID();
        UUID encounterId = UUID.randomUUID();

        EncounterRequestDto request = EncounterRequestDto.builder()
                .patientId(patientId)
                .practitionerId(practitionerId)
                .encounterDate(Instant.now())
                .reason("Annual Physical Exam")
                .build();

        EncounterResponseDto response = EncounterResponseDto.builder()
                .id(encounterId)
                .patientId(patientId)
                .patientName("John Doe")
                .practitionerId(practitionerId)
                .practitionerName("Dr. Smith")
                .status(EncounterStatus.PLANNED)
                .reason(request.getReason())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        when(encounterService.createEncounter(any(EncounterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/encounters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(encounterId.toString()))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.patientName").value("John Doe"))
                .andExpect(jsonPath("$.practitionerName").value("Dr. Smith"));
    }

    @Test
    void createEncounter_MissingPatientId_Returns400BadRequest() throws Exception {
        EncounterRequestDto request = EncounterRequestDto.builder()
                .practitionerId(UUID.randomUUID())
                .encounterDate(Instant.now())
                .build();

        mockMvc.perform(post("/api/v1/encounters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEncounter_PatientNotFound_Returns404NotFound() throws Exception {
        EncounterRequestDto request = EncounterRequestDto.builder()
                .patientId(UUID.randomUUID())
                .practitionerId(UUID.randomUUID())
                .encounterDate(Instant.now())
                .build();

        when(encounterService.createEncounter(any(EncounterRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));

        mockMvc.perform(post("/api/v1/encounters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getAllEncounters_Returns200OK() throws Exception {
        EncounterResponseDto response = EncounterResponseDto.builder()
                .id(UUID.randomUUID())
                .status(EncounterStatus.PLANNED)
                .build();

        when(encounterService.getAllEncounters()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/encounters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].status").value("PLANNED"));
    }

    @Test
    void getEncounterById_Returns200OK() throws Exception {
        UUID encounterId = UUID.randomUUID();
        EncounterResponseDto response = EncounterResponseDto.builder()
                .id(encounterId)
                .status(EncounterStatus.PLANNED)
                .build();

        when(encounterService.getEncounterById(encounterId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/encounters/{id}", encounterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(encounterId.toString()));
    }

    @Test
    void getEncounterById_NotFound_Returns404NotFound() throws Exception {
        UUID encounterId = UUID.randomUUID();
        when(encounterService.getEncounterById(encounterId))
                .thenThrow(new ResourceNotFoundException("Encounter not found with ID: " + encounterId));

        mockMvc.perform(get("/api/v1/encounters/{id}", encounterId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
