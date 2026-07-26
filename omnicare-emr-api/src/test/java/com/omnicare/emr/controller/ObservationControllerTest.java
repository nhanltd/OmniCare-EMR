package com.omnicare.emr.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicare.emr.dto.ObservationRequestDto;
import com.omnicare.emr.dto.ObservationResponseDto;
import com.omnicare.emr.exception.EncounterCancelledException;
import com.omnicare.emr.exception.GlobalExceptionHandler;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.service.ObservationService;
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

@WebMvcTest(ObservationController.class)
@Import(GlobalExceptionHandler.class)
class ObservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ObservationService observationService;

    @Test
    void createObservation_Returns201Created() throws Exception {
        UUID encounterId = UUID.randomUUID();
        UUID observationId = UUID.randomUUID();
        JsonNode vitals = objectMapper.readTree("{\"bloodPressure\": \"120/80\", \"heartRate\": 75, \"temp\": 37.0}");

        ObservationRequestDto request = ObservationRequestDto.builder()
                .encounterId(encounterId)
                .valueJson(vitals)
                .build();

        ObservationResponseDto response = ObservationResponseDto.builder()
                .id(observationId)
                .encounterId(encounterId)
                .valueJson(vitals)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        when(observationService.createObservation(any(ObservationRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(observationId.toString()))
                .andExpect(jsonPath("$.encounterId").value(encounterId.toString()))
                .andExpect(jsonPath("$.valueJson.bloodPressure").value("120/80"))
                .andExpect(jsonPath("$.valueJson.heartRate").value(75))
                .andExpect(jsonPath("$.valueJson.temp").value(37.0));
    }

    @Test
    void createObservation_MissingEncounterId_Returns400BadRequest() throws Exception {
        JsonNode vitals = objectMapper.readTree("{\"heartRate\": 75}");
        ObservationRequestDto request = ObservationRequestDto.builder()
                .valueJson(vitals)
                .build();

        mockMvc.perform(post("/api/v1/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createObservation_EncounterNotFound_Returns404NotFound() throws Exception {
        UUID encounterId = UUID.randomUUID();
        JsonNode vitals = objectMapper.readTree("{\"heartRate\": 75}");
        ObservationRequestDto request = ObservationRequestDto.builder()
                .encounterId(encounterId)
                .valueJson(vitals)
                .build();

        when(observationService.createObservation(any(ObservationRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Encounter not found with ID: " + encounterId));

        mockMvc.perform(post("/api/v1/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createObservation_CancelledEncounter_ReturnsEncounterCancelledError() throws Exception {
        UUID encounterId = UUID.randomUUID();
        JsonNode vitals = objectMapper.readTree("{\"heartRate\": 75}");
        ObservationRequestDto request = ObservationRequestDto.builder()
                .encounterId(encounterId)
                .valueJson(vitals)
                .build();

        when(observationService.createObservation(any(ObservationRequestDto.class)))
                .thenThrow(new EncounterCancelledException("Cannot record observation for cancelled encounter with ID: " + encounterId));

        mockMvc.perform(post("/api/v1/observations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Encounter Cancelled"))
                .andExpect(jsonPath("$.type").value("https://api.omnicare.com/errors/encounter-cancelled"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getObservationsByEncounterId_Returns200OK() throws Exception {
        UUID encounterId = UUID.randomUUID();
        UUID observationId = UUID.randomUUID();
        JsonNode vitals = objectMapper.readTree("{\"heartRate\": 75}");

        ObservationResponseDto response = ObservationResponseDto.builder()
                .id(observationId)
                .encounterId(encounterId)
                .valueJson(vitals)
                .build();

        when(observationService.getObservationsByEncounterId(encounterId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/observations")
                .param("encounterId", encounterId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(observationId.toString()))
                .andExpect(jsonPath("$[0].encounterId").value(encounterId.toString()));
    }
}
