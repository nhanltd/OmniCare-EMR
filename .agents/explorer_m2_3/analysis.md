# Strategy & Technical Analysis Report: Milestone M2 (Core Data Model & Persistence Configuration)

**Author**: Explorer M2 Instance 3  
**Target Project**: `omnicare-emr-api`  
**Date**: 2026-07-24  
**Status**: COMPLETE  

---

## Executive Summary

This report provides a comprehensive technical analysis and strategic roadmap for **Milestone M2: Core Data Model & Persistence Configuration**. 

The investigation covers:
1. **M1 Project Baseline Audit**: Inspection of project directory structure, `pom.xml`, `Dockerfile`, `README.md`, and workspace `docker-compose.yml`.
2. **`application.yml` Persistence Analysis**: Review of PostgreSQL datasource settings, HikariCP connection pool parameters, and Hibernate SQL logging/formatting strategies.
3. **`BaseEntity` Architectural Design**: JPA `@MappedSuperclass` specification incorporating UUID primary key generation, Spring Data JPA Auditing (`created_at`, `updated_at`), `@Version` Optimistic Locking, and `is_deleted` Soft Delete flag.
4. **`Patient` Entity Domain Specification**: JPA entity column mappings, constraints (`UNIQUE`, `NOT NULL`, string lengths), temporal data types, and UTF-8 diacritical character preservation.
5. **JPA Auditing & Configuration Blueprint**: Concrete Java code blueprints for `BaseEntity`, `Patient`, and `JpaConfig`.

---

## 1. M1 Project Baseline & Infrastructure Audit

### 1.1 Workspace Infrastructure (`docker-compose.yml`)
- **Location**: Workspace root `c:/Users/nhan/Workspace/OmniCare-EMR/docker-compose.yml`
- **Engine**: PostgreSQL 16 Alpine (`postgres:16-alpine`)
- **Port Mapping**: `5432:5432`
- **Database Credentials**:
  - `POSTGRES_DB`: `omnicare_db`
  - `POSTGRES_USER`: `omnicare_user`
  - `POSTGRES_PASSWORD`: `omnicare_pass`
- **Health Check**: `pg_isready -U omnicare_user -d omnicare_db` (interval 10s, timeout 5s, 5 retries).

### 1.2 Maven Build Configuration (`pom.xml`)
- **Spring Boot Version**: `3.2.5`
- **Java Compiler Target**: `17`
- **Dependencies Present**:
  - `spring-boot-starter-web`: RESTful web layer.
  - `spring-boot-starter-data-jpa`: Hibernate ORM, Spring Data JPA, HikariCP connection pool.
  - `spring-boot-starter-validation`: Bean Validation (JSR-380 / Jakarta Validation).
  - `org.postgresql:postgresql`: PostgreSQL JDBC Driver (runtime scope).
  - `org.projectlombok:lombok`: Code generation annotations (optional scope).
  - `spring-boot-starter-test`: Testing dependencies (JUnit 5, Mockito).

### 1.3 Package Layout Compliance
The source directory under `src/main/java/com/omnicare/emr/` adheres strictly to standard package separation:
```
com.omnicare.emr/
├── OmnicareApiApplication.java
├── config/             # Spring JPA & Auditing configuration
├── controller/         # REST API endpoints
├── dto/                # Request & Response DTOs
├── entity/             # JPA Entities (BaseEntity, Patient)
├── exception/          # GlobalExceptionHandler & Domain exceptions
├── repository/         # Spring Data JPA repositories
└── service/            # Business logic interfaces & implementations
```

---

## 2. `application.yml` Database & Persistence Strategy

### 2.1 Current Configuration Baseline
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

### 2.2 Analysis & Optimizations

1. **Auto-DDL Strategy**:
   - `spring.jpa.hibernate.ddl-auto: update` automatically generates and alters tables (such as `patient`) when entities are scanned on application boot.
   - Satisfies Requirement **R3** ("enable auto-ddl to generate tables automatically") and E2E Tier 1 DB schema assertions.

2. **HikariCP Connection Pool Configuration**:
   - Spring Data JPA defaults to HikariCP, but no pool size or timeout limits are explicitly configured in the default `application.yml`.
   - **Recommended Additions**:
     ```yaml
     spring:
       datasource:
         hikari:
           pool-name: OmniCareHikariPool
           maximum-pool-size: 10
           minimum-idle: 5
           idle-timeout: 300000      # 5 minutes
           connection-timeout: 20000 # 20 seconds
           max-lifetime: 1200000     # 20 minutes
     ```

3. **Hibernate SQL Logging & Binding Parameters**:
   - Currently `show-sql: true` outputs unformatted raw SQL statements directly to standard output, bypassing Logback/SLF4J.
   - For Hibernate 6 (Spring Boot 3.x), SQL parameter bindings (`?`) are logged via `org.hibernate.orm.jdbc.bind`.
   - **Recommended Logging Configuration**:
     ```yaml
     spring:
       jpa:
         show-sql: false # Avoid duplicated console output
         properties:
           hibernate:
             format_sql: true
             highlight_sql: true

     logging:
       level:
         org.hibernate.SQL: DEBUG
         org.hibernate.orm.jdbc.bind: TRACE
     ```

---

## 3. Core Data Model & JPA Annotation Blueprint

### 3.1 `BaseEntity` Architectural Specification

`BaseEntity` must be defined as an abstract `@MappedSuperclass` under package `com.omnicare.emr.entity`.

#### Mandatory Column Mappings & Metadata:

| Java Field | DB Column Name | SQL Type | JPA Annotations | Description & Constraints |
|---|---|---|---|---|
| `id` | `id` | `UUID` | `@Id`, `@GeneratedValue(strategy = GenerationType.UUID)`, `@Column(name = "id", nullable = false, updatable = false)` | Primary key. Automatically generated RFC 4122 UUID v4. |
| `createdAt` | `created_at` | `TIMESTAMP` | `@CreatedDate`, `@Column(name = "created_at", nullable = false, updatable = false)` | Audit creation timestamp populated automatically by Spring Data JPA Auditing. |
| `updatedAt` | `updated_at` | `TIMESTAMP` | `@LastModifiedDate`, `@Column(name = "updated_at", nullable = false)` | Audit update timestamp modified on entity mutations. |
| `version` | `version` | `BIGINT` / `INTEGER` | `@Version`, `@Column(name = "version", nullable = false)` | Optimistic locking version counter. Initial value `0`. |
| `isDeleted` | `is_deleted` | `BOOLEAN` | `@Column(name = "is_deleted", nullable = false)` | Soft delete flag. Defaults to `false`. |

#### Essential Code Pattern for `BaseEntity`:
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

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
    @Column(name = "id", nullable = false, updatable = false)
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

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}
```

### 3.2 `Patient` Entity Specification

`Patient` entity inherits from `BaseEntity`, annotated with `@Entity` and mapped to table `patient`.

#### Mandatory Column Mappings & Metadata:

| Java Field | DB Column Name | SQL Type | JPA Annotations | Description & Constraints |
|---|---|---|---|---|
| `identifier` | `identifier` | `VARCHAR(20)` | `@Column(name = "identifier", length = 20, nullable = false, unique = true)` | Vietnamese CCCD or internal medical ID. Unique constraint enforced at DB level. |
| `fullName` | `full_name` | `VARCHAR(100)` | `@Column(name = "full_name", length = 100, nullable = false)` | Patient's full name. Supports full UTF-8 Vietnamese diacritical marks. |
| `gender` | `gender` | `VARCHAR(10)` | `@Column(name = "gender", length = 10)` | Gender string (`male`, `female`, `other`). |
| `birthDate` | `birth_date` | `DATE` | `@Column(name = "birth_date")` | Date of birth (`YYYY-MM-DD`). |
| `phoneNumber` | `phone_number` | `VARCHAR(15)` | `@Column(name = "phone_number", length = 15)` | Contact phone number. |

#### Essential Code Pattern for `Patient`:
```java
package com.omnicare.emr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "patient")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Patient extends BaseEntity {

    @Column(name = "identifier", length = 20, nullable = false, unique = true)
    private String identifier;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;
}
```

### 3.3 JPA Auditing Configuration Specification

To ensure `@CreatedDate` and `@LastModifiedDate` annotations are processed by Spring Data JPA during entity creation and modification, a dedicated configuration class must be declared under package `com.omnicare.emr.config`:

```java
package com.omnicare.emr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

---

## 4. E2E Compliance & Verification Matrix

The design specified in this report satisfies all 10 column metadata and constraint assertions enforced by `e2e-tests/test_tier1_infrastructure.py` and `e2e-tests/verify_db_state.sql`:

| Column | Data Type | Nullable | Unique | Verification E2E Tier |
|---|---|---|---|---|
| `id` | `UUID` | `NOT NULL` | `PRIMARY KEY` | Tier 1 (Schema) & Tier 2 (UUID format) |
| `created_at` | `TIMESTAMP` | `NOT NULL` | No | Tier 1 (Schema) & Tier 2 (Audit timestamp) |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | No | Tier 1 (Schema) & Tier 2 (Audit timestamp) |
| `version` | `BIGINT` | `NOT NULL` | No | Tier 1 (Schema), Tier 2 (`version=0`), Tier 4 |
| `is_deleted` | `BOOLEAN` | `NOT NULL` | No | Tier 1 (Schema), Tier 2 (`is_deleted=false`), Tier 4 |
| `identifier` | `VARCHAR(20)` | `NOT NULL` | `UNIQUE` | Tier 1 (Schema), Tier 3 (Duplicate CCCD 409/400) |
| `full_name` | `VARCHAR(100)` | `NOT NULL` | No | Tier 1 (Schema), Tier 4 (UTF-8 diacritics) |
| `gender` | `VARCHAR(10)` | `NULL` | No | Tier 1 (Schema) & Tier 2 (Patient payload) |
| `birth_date` | `DATE` | `NULL` | No | Tier 1 (Schema) & Tier 2 (Patient payload) |
| `phone_number` | `VARCHAR(15)` | `NULL` | No | Tier 1 (Schema) & Tier 2 (Patient payload) |

---

## 5. Implementation Roadmap for Implementer Agent

1. **Step 1**: Create `JpaConfig.java` in `com.omnicare.emr.config` with `@EnableJpaAuditing`.
2. **Step 2**: Create `BaseEntity.java` in `com.omnicare.emr.entity` with `@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`, `id` (UUID), `createdAt` (`@CreatedDate`), `updatedAt` (`@LastModifiedDate`), `version` (`@Version`), and `isDeleted`.
3. **Step 3**: Create `Patient.java` in `com.omnicare.emr.entity` inheriting `BaseEntity`, with `@Entity`, `@Table(name = "patient")`, `identifier`, `fullName`, `gender`, `birthDate`, `phoneNumber`.
4. **Step 4**: Update `application.yml` with HikariCP connection pool settings and formatted SQL logging.
5. **Step 5**: Run `mvn clean compile` to ensure 0 compilation errors.
