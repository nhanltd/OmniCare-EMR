# Milestone M3 Technical Analysis & Java Code Blueprints

## 1. Executive Summary & Scope

This document presents the complete technical exploration and production-ready code blueprints for **Milestone M3 (End-to-End API Implementation)** of the `omnicare-emr-api` project.

Milestone M3 implements the core Patient registration flow (`POST /api/v1/patients`), establishing end-to-end data flow from HTTP request, validation, service business logic, transactional database persistence via Spring Data JPA, error handling, and JSON response formatting.

The implementation strictly satisfies all database schema constraints, entity structure (`Patient` extending `BaseEntity`), and the end-to-end integration test suite located in `e2e-tests/`.

---

## 2. Codebase & E2E Test Compatibility Analysis

### 2.1 Entity & Schema Mapping
The existing entity model consists of:
- `BaseEntity`: Contains primary key `id` (UUID generated via `@GeneratedValue(strategy = GenerationType.UUID)`), audit timestamps `createdAt` and `updatedAt` (`Instant`), `@Version Long version`, and `boolean isDeleted` (default `false`).
- `Patient`: Extends `BaseEntity`, mapped to table `patient`. Fields:
  - `identifier` (`String`, nullable=false, unique=true, length=20)
  - `fullName` (`String`, nullable=false, length=100)
  - `gender` (`String`, length=10)
  - `birthDate` (`LocalDate`)
  - `phoneNumber` (`String`, length=15)

### 2.2 E2E Test Compatibility Checklist
1. **Tier 1 (Infrastructure & Schema)**: Requires database table `patient` with columns `id`, `created_at`, `updated_at`, `version`, `is_deleted`, `identifier`, `full_name`, `gender`, `birth_date`, `phone_number`. JPA entity structure matches this table schema exactly. `@EnableJpaAuditing` is enabled in `JpaConfig.java`.
2. **Tier 2 (Happy Path Registration)**:
   - Request: `POST /api/v1/patients` with JSON payload `{ identifier, fullName, gender, birthDate, phoneNumber }`.
   - Response: Status `201 Created` (or 200). Response JSON containing `id` (valid UUID string).
   - DB state assertion: Record inserted in `patient` table with matching `identifier`, `full_name`, `version = 0`, `is_deleted = false`, `created_at` and `updated_at` populated.
3. **Tier 3 (Validation & Duplicates)**:
   - Missing required field (`identifier`): API must return `400 Bad Request` with an `ErrorResponseDto` containing `status` and `error` keys.
   - Duplicate identifier attempt: API must return `409 Conflict` (or 400) with `ErrorResponseDto`, and DB must contain exactly 1 record.
4. **Tier 4 (Integrity & Diacritics)**:
   - Full support for UTF-8 Vietnamese diacritics (e.g., `"Nguyễn Thị Ánh Tuyết"`). Spring MVC and Jackson handle UTF-8 string encoding natively.
   - Unique UUID generation per request.

---

## 3. Production-Ready Java Code Blueprints

### 3.1 Repository Layer

#### `com.omnicare.emr.repository.PatientRepository`
```java
package com.omnicare.emr.repository;

import com.omnicare.emr.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA Repository for Patient entity.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    /**
     * Check if a patient exists with the given identifier (CCCD).
     *
     * @param identifier the patient CCCD/identifier
     * @return true if patient exists, false otherwise
     */
    boolean existsByIdentifier(String identifier);
}
```

---

### 3.2 Data Transfer Objects (DTOs)

#### `com.omnicare.emr.dto.PatientRequestDto`
```java
package com.omnicare.emr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO for Patient creation request payload.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequestDto {

    @NotBlank(message = "Identifier is required")
    @Size(min = 9, max = 20, message = "Identifier must be between 9 and 20 characters")
    private String identifier;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Size(max = 10, message = "Gender must not exceed 10 characters")
    private String gender;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phoneNumber;
}
```

#### `com.omnicare.emr.dto.PatientResponseDto`
```java
package com.omnicare.emr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for Patient creation response body.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponseDto {

    private UUID id;
    private String identifier;
    private String fullName;
    private String gender;
    private LocalDate birthDate;
    private String phoneNumber;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    @JsonProperty("isDeleted")
    private boolean isDeleted;
}
```

#### `com.omnicare.emr.dto.ErrorResponseDto`
```java
package com.omnicare.emr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Standardized API Error Response DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
```

---

### 3.3 Exception Handling Layer

#### `com.omnicare.emr.exception.DuplicateResourceException`
```java
package com.omnicare.emr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to create a resource that already exists.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### `com.omnicare.emr.exception.GlobalExceptionHandler`
```java
package com.omnicare.emr.exception;

import com.omnicare.emr.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Controller advice for handling global application exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String validationMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(validationMessages.isEmpty() ? "Validation failed" : validationMessages)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(
            Exception ex, HttpServletRequest request) {
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
```

---

### 3.4 Service Layer

#### `com.omnicare.emr.service.PatientService`
```java
package com.omnicare.emr.service;

import com.omnicare.emr.dto.PatientRequestDto;
import com.omnicare.emr.dto.PatientResponseDto;

/**
 * Service interface for Patient domain operations.
 */
public interface PatientService {

    /**
     * Creates a new patient record after checking for identifier uniqueness.
     *
     * @param requestDto patient creation details
     * @return created patient response details
     */
    PatientResponseDto createPatient(PatientRequestDto requestDto);
}
```

#### `com.omnicare.emr.service.impl.PatientServiceImpl`
```java
package com.omnicare.emr.service.impl;

import com.omnicare.emr.dto.PatientRequestDto;
import com.omnicare.emr.dto.PatientResponseDto;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.exception.DuplicateResourceException;
import com.omnicare.emr.repository.PatientRepository;
import com.omnicare.emr.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of {@link PatientService}.
 */
@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    @Override
    @Transactional
    public PatientResponseDto createPatient(PatientRequestDto requestDto) {
        if (patientRepository.existsByIdentifier(requestDto.getIdentifier())) {
            throw new DuplicateResourceException(
                    "Patient with identifier '" + requestDto.getIdentifier() + "' already exists"
            );
        }

        Patient patient = Patient.builder()
                .identifier(requestDto.getIdentifier())
                .fullName(requestDto.getFullName())
                .gender(requestDto.getGender())
                .birthDate(requestDto.getBirthDate())
                .phoneNumber(requestDto.getPhoneNumber())
                .build();

        Patient savedPatient = patientRepository.save(patient);

        return mapToResponseDto(savedPatient);
    }

    private PatientResponseDto mapToResponseDto(Patient patient) {
        return PatientResponseDto.builder()
                .id(patient.getId())
                .identifier(patient.getIdentifier())
                .fullName(patient.getFullName())
                .gender(patient.getGender())
                .birthDate(patient.getBirthDate())
                .phoneNumber(patient.getPhoneNumber())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .version(patient.getVersion())
                .isDeleted(patient.isDeleted())
                .build();
    }
}
```

---

### 3.5 Controller Layer

#### `com.omnicare.emr.controller.PatientController`
```java
package com.omnicare.emr.controller;

import com.omnicare.emr.dto.PatientRequestDto;
import com.omnicare.emr.dto.PatientResponseDto;
import com.omnicare.emr.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing patient resources.
 */
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    /**
     * Endpoint to register a new patient.
     *
     * @param request JSON payload containing patient creation data
     * @return 201 Created status with saved patient details
     */
    @PostMapping
    public ResponseEntity<PatientResponseDto> createPatient(@Valid @RequestBody PatientRequestDto request) {
        PatientResponseDto response = patientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

---

## 4. Test Suite Blueprints

### 4.1 Service Unit Test (`PatientServiceImplTest.java`)
```java
package com.omnicare.emr.service;

import com.omnicare.emr.dto.PatientRequestDto;
import com.omnicare.emr.dto.PatientResponseDto;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.exception.DuplicateResourceException;
import com.omnicare.emr.repository.PatientRepository;
import com.omnicare.emr.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientServiceImpl patientService;

    private PatientRequestDto requestDto;
    private Patient savedPatient;

    @BeforeEach
    void setUp() {
        requestDto = PatientRequestDto.builder()
                .identifier("079123456789")
                .fullName("Nguyễn Văn A")
                .gender("male")
                .birthDate(LocalDate.of(1990, 1, 1))
                .phoneNumber("+84901234567")
                .build();

        savedPatient = Patient.builder()
                .id(UUID.randomUUID())
                .identifier("079123456789")
                .fullName("Nguyễn Văn A")
                .gender("male")
                .birthDate(LocalDate.of(1990, 1, 1))
                .phoneNumber("+84901234567")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .isDeleted(false)
                .build();
    }

    @Test
    void createPatient_Success() {
        when(patientRepository.existsByIdentifier(requestDto.getIdentifier())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);

        PatientResponseDto response = patientService.createPatient(requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedPatient.getId());
        assertThat(response.getIdentifier()).isEqualTo("079123456789");
        assertThat(response.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(response.getVersion()).isEqualTo(0L);
        assertThat(response.isDeleted()).isFalse();

        verify(patientRepository).existsByIdentifier(requestDto.getIdentifier());
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    void createPatient_DuplicateIdentifier_ThrowsDuplicateResourceException() {
        when(patientRepository.existsByIdentifier(requestDto.getIdentifier())).thenReturn(true);

        assertThatThrownBy(() -> patientService.createPatient(requestDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("079123456789");

        verify(patientRepository).existsByIdentifier(requestDto.getIdentifier());
        verify(patientRepository, never()).save(any(Patient.class));
    }
}
```

### 4.2 Web Layer Controller Test (`PatientControllerTest.java`)
```java
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
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
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Patient with identifier '079123456789' already exists"));
    }
}
```

---

## 5. Verification Strategy

1. **Unit & Slice Testing**: Execute `./mvnw test` or `mvn test` in `omnicare-emr-api` directory.
2. **E2E Integration Verification**:
   - Start PostgreSQL and `omnicare-emr-api` instance.
   - Run PyTest runner against `e2e-tests/`: `pytest test_tier1_infrastructure.py test_tier2_happy_path.py test_tier3_validation.py test_tier4_integrity.py`.
