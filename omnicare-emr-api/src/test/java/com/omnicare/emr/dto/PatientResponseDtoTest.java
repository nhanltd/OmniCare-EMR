package com.omnicare.emr.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PatientResponseDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should serialize isDeleted field as 'isDeleted' in JSON output")
    void testJsonSerialization_isDeletedKeyPresent() throws Exception {
        PatientResponseDto dto = PatientResponseDto.builder()
                .id(UUID.randomUUID())
                .identifier("079123456789")
                .fullName("Test Patient")
                .gender("male")
                .birthDate(LocalDate.of(1990, 1, 1))
                .phoneNumber("+84901234567")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(1L)
                .isDeleted(false)
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"isDeleted\":false");
        assertThat(json).doesNotContain("\"deleted\":");
    }

    @Test
    @DisplayName("Should deserialize JSON with 'isDeleted' field into PatientResponseDto")
    void testJsonDeserialization_isDeletedKeyParsed() throws Exception {
        String json = """
                {
                    "id": "123e4567-e89b-12d3-a456-426614174000",
                    "identifier": "079123456789",
                    "fullName": "Test Patient",
                    "gender": "male",
                    "birthDate": "1990-01-01",
                    "phoneNumber": "+84901234567",
                    "version": 1,
                    "isDeleted": true
                }
                """;

        PatientResponseDto dto = objectMapper.readValue(json, PatientResponseDto.class);

        assertThat(dto.isDeleted()).isTrue();
    }
}
