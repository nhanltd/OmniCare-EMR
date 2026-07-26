# Milestone M2 Technical Strategy & Code Analysis

## 1. Requirement & Architecture Analysis

Milestone M2 focuses on the Core Data Model & Persistence Configuration for the OmniCare EMR core backend application.

### Key Requirements (Requirement R3):
1. **Abstract Base Entity (`BaseEntity`)**:
   - `id`: UUID, Primary Key (`@Id`, `@GeneratedValue(strategy = GenerationType.UUID)`), non-updatable, non-nullable.
   - `createdAt`: `java.time.Instant`, automatically populated upon creation (`@CreatedDate`), mapped to column `created_at`, non-updatable, non-nullable.
   - `updatedAt`: `java.time.Instant`, automatically populated upon creation/update (`@LastModifiedDate`), mapped to column `updated_at`, non-nullable.
   - `version`: `Long`, annotated with `@Version` for Optimistic Locking, mapped to column `version`, non-nullable (initial DB default 0).
   - `isDeleted`: `boolean`, mapped to column `is_deleted`, non-nullable, default `false`.
   - Annotated with `@MappedSuperclass` and `@EntityListeners(AuditingEntityListener.class)`.

2. **Patient Entity (`Patient`)**:
   - Extends `BaseEntity`.
   - Mapped to table `patient`.
   - `identifier`: `String`, mapped to column `identifier`, `VARCHAR(20)`, `UNIQUE`, `NOT NULL`.
   - `fullName`: `String`, mapped to column `full_name`, `VARCHAR(100)`, `NOT NULL`.
   - `gender`: `String`, mapped to column `gender`, `VARCHAR(10)`.
   - `birthDate`: `java.time.LocalDate`, mapped to column `birth_date`.
   - `phoneNumber`: `String`, mapped to column `phone_number`, `VARCHAR(15)`.

3. **JPA Auditing Configuration (`JpaConfig`)**:
   - Class `com.omnicare.emr.config.JpaConfig` annotated with `@Configuration` and `@EnableJpaAuditing` to enable JPA entity listeners for `@CreatedDate` and `@LastModifiedDate`.

4. **Persistence Configuration (`application.yml`)**:
   - PostgreSQL connection settings pointing to database `omnicare_db`, user `omnicare_user`, password `omnicare_pass` on host `localhost:5432`.
   - `hibernate.ddl-auto: update` to ensure automatic DDL schema updates upon application startup.

---

## 2. Project Inspection Findings

- **Project Root**: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api`
- **Package Base**: `com.omnicare.emr`
- **Existing Directories**:
  - `src/main/java/com/omnicare/emr/entity/`
  - `src/main/java/com/omnicare/emr/config/`
  - `src/main/resources/application.yml`
- **Dependencies**: `pom.xml` includes `spring-boot-starter-data-jpa`, `postgresql`, `lombok` (configured with `annotationProcessorPaths`). No additional Maven dependencies needed.

---

## 3. Code Templates for Implementation

### 3.1. `BaseEntity.java`
**Target File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/entity/BaseEntity.java`

```java
package com.omnicare.emr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Base abstract entity containing auditing, ID generation, and concurrency control fields.
 * Mapped as superclass for all business entities in OmniCare EMR.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}
```

### 3.2. `Patient.java`
**Target File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/entity/Patient.java`

```java
package com.omnicare.emr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Patient entity representing the patient table in OmniCare EMR.
 */
@Entity
@Table(
    name = "patient",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_patient_identifier", columnNames = {"identifier"})
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Patient extends BaseEntity {

    @Column(name = "identifier", nullable = false, unique = true, length = 20)
    private String identifier;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;
}
```

### 3.3. `JpaConfig.java`
**Target File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/config/JpaConfig.java`

```java
package com.omnicare.emr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Configuration enabling automatic auditing (@CreatedDate, @LastModifiedDate).
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

### 3.4. `application.yml`
**Target File**: `omnicare-emr-api/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: omnicare-emr-api

  datasource:
    url: jdbc:postgresql://localhost:5432/omnicare_db
    username: omnicare_user
    password: omnicare_pass
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

---

## 4. Verification & Validation Strategy

1. **Compilation Check**:
   - Run `mvn clean compile` in `omnicare-emr-api` directory to verify Java entity annotations, Lombok processing, and Spring Data JPA class resolution.
2. **Database Schema Verification**:
   - Start PostgreSQL via docker-compose (if running).
   - Application startup will trigger Hibernate DDL auto update to create `patient` table with exact columns:
     - `id` (UUID PK)
     - `identifier` (VARCHAR(20) UNIQUE NOT NULL)
     - `full_name` (VARCHAR(100) NOT NULL)
     - `gender` (VARCHAR(10))
     - `birth_date` (DATE)
     - `phone_number` (VARCHAR(15))
     - `created_at` (TIMESTAMP NOT NULL)
     - `updated_at` (TIMESTAMP NOT NULL)
     - `version` (INT8/BIGINT NOT NULL)
     - `is_deleted` (BOOL NOT NULL DEFAULT false)
3. **E2E SQL Schema Compatibility**:
   - Confirmed schema alignment with `e2e-tests/verify_db_state.sql` and `e2e-tests/test_tier2_happy_path.py`.
