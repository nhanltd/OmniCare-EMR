package com.omnicare.emr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicare.emr.dto.PatientRequestDto;
import com.omnicare.emr.dto.PatientResponseDto;
import com.omnicare.emr.exception.DuplicateResourceException;
import com.omnicare.emr.exception.GlobalExceptionHandler;
import com.omnicare.emr.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
@Import(GlobalExceptionHandler.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientService patientService;

    @Test
    void createPatient_Returns201Created() throws Exception {
        PatientRequestDto request = PatientRequestDto.builder()
                .identifier("079123456789")
                .fullName("Nguyễn Thị Ánh Tuyết")
                .gender("female")
                .birthDate(LocalDate.of(1995, 5, 15))
                .phoneNumber("+84987654321")
                .build();

        PatientResponseDto response = PatientResponseDto.builder()
                .id(UUID.randomUUID())
                .identifier(request.getIdentifier())
                .fullName(request.getFullName())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .phoneNumber(request.getPhoneNumber())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .isDeleted(false)
                .build();

        when(patientService.createPatient(any(PatientRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.identifier").value("079123456789"))
                .andExpect(jsonPath("$.fullName").value("Nguyễn Thị Ánh Tuyết"))
                .andExpect(jsonPath("$.isDeleted").value(false));
    }

    @Test
    void createPatient_MissingIdentifier_Returns400BadRequest() throws Exception {
        PatientRequestDto invalidRequest = PatientRequestDto.builder()
                .fullName("Missing Identifier Person")
                .gender("female")
                .build();

        mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createPatient_DuplicateIdentifier_Returns409Conflict() throws Exception {
        PatientRequestDto request = PatientRequestDto.builder()
                .identifier("079123456789")
                .fullName("Duplicate Person")
                .build();

        when(patientService.createPatient(any(PatientRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Patient with identifier '079123456789' already exists"));

        mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Duplicate Resource"))
                .andExpect(jsonPath("$.detail").value("Patient with identifier '079123456789' already exists"));
    }

    @Test
    void createPatient_BirthDatePresent_Returns201Created() throws Exception {
        PatientRequestDto request = PatientRequestDto.builder()
                .identifier("079123456789")
                .fullName("Newborn Person")
                .birthDate(LocalDate.now())
                .build();

        PatientResponseDto response = PatientResponseDto.builder()
                .id(UUID.randomUUID())
                .identifier(request.getIdentifier())
                .fullName(request.getFullName())
                .birthDate(request.getBirthDate())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .isDeleted(false)
                .build();

        when(patientService.createPatient(any(PatientRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createPatient_DataIntegrityViolation_Returns409Conflict() throws Exception {
        PatientRequestDto request = PatientRequestDto.builder()
                .identifier("079123456789")
                .fullName("Duplicate Person")
                .build();

        when(patientService.createPatient(any(PatientRequestDto.class)))
                .thenThrow(new DataIntegrityViolationException("Database constraint violation"));

        mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Data Integrity Violation"))
                .andExpect(jsonPath("$.detail").value("Duplicate entity or data integrity violation"));
    }

    @Test
    void createPatient_MalformedJson_Returns400BadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
