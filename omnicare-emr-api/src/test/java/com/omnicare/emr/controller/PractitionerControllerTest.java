package com.omnicare.emr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicare.emr.dto.PractitionerRequestDto;
import com.omnicare.emr.dto.PractitionerResponseDto;
import com.omnicare.emr.entity.PractitionerType;
import com.omnicare.emr.exception.DuplicateResourceException;
import com.omnicare.emr.exception.GlobalExceptionHandler;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.service.PractitionerService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PractitionerController.class)
@Import(GlobalExceptionHandler.class)
class PractitionerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PractitionerService practitionerService;

    private UUID sampleId;
    private PractitionerRequestDto validRequest;
    private PractitionerResponseDto validResponse;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();

        validRequest = PractitionerRequestDto.builder()
                .practitionerCode("PRAC-001")
                .fullName("Dr. Sarah Connor")
                .specialty("CARDIOLOGY")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+1-555-0101")
                .email("sarah.connor@omnicare.com")
                .build();

        validResponse = PractitionerResponseDto.builder()
                .id(sampleId)
                .practitionerCode("PRAC-001")
                .fullName("Dr. Sarah Connor")
                .specialty("CARDIOLOGY")
                .practitionerType(PractitionerType.DOCTOR)
                .phone("+1-555-0101")
                .email("sarah.connor@omnicare.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .isDeleted(false)
                .build();
    }

    @Test
    void createPractitioner_Returns201Created() throws Exception {
        when(practitionerService.createPractitioner(any(PractitionerRequestDto.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/v1/practitioners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(sampleId.toString()))
                .andExpect(jsonPath("$.practitionerCode").value("PRAC-001"))
                .andExpect(jsonPath("$.fullName").value("Dr. Sarah Connor"))
                .andExpect(jsonPath("$.specialty").value("CARDIOLOGY"))
                .andExpect(jsonPath("$.practitionerType").value("DOCTOR"))
                .andExpect(jsonPath("$.isDeleted").value(false));
    }

    @Test
    void createPractitioner_MissingRequiredFields_Returns400BadRequest() throws Exception {
        PractitionerRequestDto invalidRequest = PractitionerRequestDto.builder()
                .phone("+1-555-0101")
                .build();

        mockMvc.perform(post("/api/v1/practitioners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPractitioner_DuplicateCode_Returns409Conflict() throws Exception {
        when(practitionerService.createPractitioner(any(PractitionerRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Practitioner with code 'PRAC-001' already exists"));

        mockMvc.perform(post("/api/v1/practitioners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Resource"))
                .andExpect(jsonPath("$.detail").value("Practitioner with code 'PRAC-001' already exists"));
    }

    @Test
    void getAllPractitioners_Returns200OK() throws Exception {
        when(practitionerService.getAllPractitioners()).thenReturn(List.of(validResponse));

        mockMvc.perform(get("/api/v1/practitioners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sampleId.toString()))
                .andExpect(jsonPath("$[0].practitionerCode").value("PRAC-001"));
    }

    @Test
    void getPractitionerById_Success_Returns200OK() throws Exception {
        when(practitionerService.getPractitionerById(sampleId)).thenReturn(validResponse);

        mockMvc.perform(get("/api/v1/practitioners/{id}", sampleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleId.toString()))
                .andExpect(jsonPath("$.practitionerCode").value("PRAC-001"));
    }

    @Test
    void getPractitionerById_NotFound_Returns404NotFound() throws Exception {
        when(practitionerService.getPractitionerById(sampleId))
                .thenThrow(new ResourceNotFoundException("Practitioner not found with ID: " + sampleId));

        mockMvc.perform(get("/api/v1/practitioners/{id}", sampleId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Practitioner not found with ID: " + sampleId));
    }

    @Test
    void updatePractitioner_Success_Returns200OK() throws Exception {
        when(practitionerService.updatePractitioner(eq(sampleId), any(PractitionerRequestDto.class))).thenReturn(validResponse);

        mockMvc.perform(put("/api/v1/practitioners/{id}", sampleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleId.toString()))
                .andExpect(jsonPath("$.practitionerCode").value("PRAC-001"));
    }

    @Test
    void updatePractitioner_NotFound_Returns404NotFound() throws Exception {
        when(practitionerService.updatePractitioner(eq(sampleId), any(PractitionerRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Practitioner not found with ID: " + sampleId));

        mockMvc.perform(put("/api/v1/practitioners/{id}", sampleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void updatePractitioner_DuplicateCode_Returns409Conflict() throws Exception {
        when(practitionerService.updatePractitioner(eq(sampleId), any(PractitionerRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Practitioner with code 'PRAC-001' already exists"));

        mockMvc.perform(put("/api/v1/practitioners/{id}", sampleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Resource"));
    }

    @Test
    void deletePractitioner_Success_Returns204NoContent() throws Exception {
        doNothing().when(practitionerService).deletePractitioner(sampleId);

        mockMvc.perform(delete("/api/v1/practitioners/{id}", sampleId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePractitioner_NotFound_Returns404NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Practitioner not found with ID: " + sampleId))
                .when(practitionerService).deletePractitioner(sampleId);

        mockMvc.perform(delete("/api/v1/practitioners/{id}", sampleId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
