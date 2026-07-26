# Strategy & Technical Analysis Report: Core Data Model & Persistence Configuration (Milestone M2)

**Author:** Explorer M2 Instance 2  
**Date:** 2026-07-24  
**Target Project:** OmniCare EMR (`omnicare-emr-api`)  
**Scope:** Milestone M2 — `BaseEntity`, `Patient` entity, JPA annotations, Lombok `@SuperBuilder` design, auditing, UUID strategy, optimistic locking, soft delete defaults, and database persistence configuration.

---

## 1. Project Baseline & File Structure Inspection (M1 State)

### 1.1 Existing Structure & Dependencies
The `omnicare-emr-api` project was initialized in M1 under `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api`.

Key Maven configuration (`pom.xml`):
- **Spring Boot Version:** `3.2.5` (Jakarta EE / Spring Data JPA 3.2 / Hibernate 6.4+)
- **Java Version:** `17`
- **Core Dependencies:**
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-validation`
  - `postgresql` (JDBC Driver)
  - `lombok` (with `maven-compiler-plugin` annotation processor enabled)

### 1.2 Database & Persistence Configuration (`application.yml`)
Located at `omnicare-emr-api/src/main/resources/application.yml`:
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
*Analysis:* The datasource configuration correctly connects to PostgreSQL container on port `5432`. Hibernate's `ddl-auto: update` will automatically create/update physical table schemas (`patient` table and columns) matching entity JPA annotations upon application boot.

### 1.3 Identified Gap
Currently, `omnicare-emr-api/src/main/java/com/omnicare/emr/entity` contains only `package-info.java`. `OmnicareApiApplication.java` does not enable JPA Auditing. To enable automatic handling of `@CreatedDate` and `@LastModifiedDate`, a `@EnableJpaAuditing` configuration class must be introduced.

---

## 2. BaseEntity Architectural & JPA Annotation Analysis

Requirement **R3** and `knowledge/OMNICARE-EMR_Database_Design.md` dictate an abstract `BaseEntity` with 5 administrative audit fields: `id` (UUID), `createdAt`, `updatedAt`, `version` (Optimistic Locking), and `isDeleted` (Soft Delete).

### 2.1 MappedSuperclass & Auditing Listener
- **`@MappedSuperclass`**: Required on `BaseEntity` so that child entities (`Patient`) inherit its table mapping fields without generating a separate single-table or joined-table inheritance hierarchy in PostgreSQL.
- **`@EntityListeners(AuditingEntityListener.class)`**: Hooks into Spring Data JPA lifecycle events to set timestamps on `@PrePersist` and `@PreUpdate`.
- **`@EnableJpaAuditing`**: Must be placed on a `@Configuration` class in package `com.omnicare.emr.config` (e.g. `JpaAuditingConfig.java`).

### 2.2 UUID Generation Strategy
- **Standard JPA 3.0 / Hibernate 6 Native Strategy**:
  ```java
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;
  ```
- **Rationale**: `GenerationType.UUID` is standard in Jakarta Persistence 3.0 (Spring Boot 3+). It automatically generates non-sequential RFC 4122 UUID v4 primary keys natively in Java/Hibernate before issuing SQL INSERT statements, preventing sequential ID enumeration attacks while avoiding legacy `@GenericGenerator` annotations.

### 2.3 Audit Timestamps (`createdAt`, `updatedAt`)
- **`createdAt`**:
  ```java
  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  ```
- **`updatedAt`**:
  ```java
  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
  ```
- **Data Type:** `java.time.Instant` is standard for UTC timestamps in modern Spring Boot 3 application design, mapping directly to PostgreSQL `TIMESTAMP` / `TIMESTAMPTZ`.

### 2.4 Optimistic Locking (`version`)
- **Annotation & Field Mapping**:
  ```java
  @Version
  @Column(name = "version", nullable = false)
  @Builder.Default
  private Long version = 0L;
  ```
- **Semantics & Purpose**:
  - `version` is initialized to `0` upon entity persistence.
  - On every UPDATE, Hibernate automatically checks `WHERE id = ? AND version = ?` and increments `version` by 1.
  - If a concurrent write occurs, Hibernate throws `OptimisticLockException` (translated by Spring into `ObjectOptimisticLockingFailureException`), preventing lost updates when two medical practitioners attempt to modify medical records simultaneously.

### 2.5 Soft Delete Defaults (`isDeleted`)
- **Annotation & Field Mapping**:
  ```java
  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean isDeleted = false;
  ```
- **Soft Delete Filtering Options**:
  1. **Option A (Hibernate 6 `@SQLRestriction` & `@SQLDelete`)**:
     Placed on child entities (e.g., `@Patient`):
     ```java
     @SQLDelete(sql = "UPDATE patient SET is_deleted = true WHERE id = ? AND version = ?")
     @SQLRestriction("is_deleted = false")
     ```
     *Note:* `@SQLRestriction` replaces the deprecated Hibernate `@Where` annotation in Hibernate 6.3+.
  2. **Option B (Hibernate 6.4+ `@SoftDelete`)**:
     ```java
     @SoftDelete(columnName = "is_deleted")
     ```
     *Note:* `@SoftDelete` is supported in Spring Boot 3.2.5 (Hibernate 6.4+) and automatically manages the boolean soft delete flag and default query restrictions across all finder methods.

---

## 3. Patient Entity & Lombok `@SuperBuilder` Design Analysis

### 3.1 Entity Mapping & Table Annotations
- **Class Annotations**:
  ```java
  @Entity
  @Table(name = "patient")
  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public class Patient extends BaseEntity
  ```
- **Table Name**: Explicitly set to `"patient"` (`@Table(name = "patient")`) matching database design spec section 2.1 and `TEST_READY.md` table assertions.

### 3.2 Lombok `@SuperBuilder` vs `@Builder` under JPA Inheritance
- **Problem with `@Builder`**: Standard Lombok `@Builder` does not support class inheritance. If `@Builder` is placed on `Patient`, the generated builder will ignore fields declared in `BaseEntity` (`id`, `createdAt`, `updatedAt`, `version`, `isDeleted`), preventing builders from initializing or copying base administrative fields.
- **Solution with `@SuperBuilder`**: Placing `@SuperBuilder` on BOTH `BaseEntity` (`@MappedSuperclass`) AND `Patient` (`@Entity`) allows Lombok to generate builder interfaces that properly expose inherited parent fields alongside `Patient` specific fields.
- **Constructor Requirements**: Combining `@SuperBuilder` with `@NoArgsConstructor` and `@AllArgsConstructor` ensures full compatibility with JPA entity instantiation requirements (JPA requires a default no-arg constructor).

### 3.3 JPA Entity Identity & Lombok Pitfalls
- **Avoid `@Data` and `@EqualsAndHashCode` on JPA Entities**:
  - Lombok `@Data` generates `hashCode()` and `equals()` using all fields. For JPA entities, this leads to critical issues:
    1. Entity hash codes change when entities transition from transient (id is `null`) to persisted state (id assigned), breaking `HashSet` / `HashMap` collections.
    2. Lazy-loaded associations (in future entities like `Encounter` or `Observation`) get initialized prematurely during `equals()` / `toString()`, leading to N+1 performance issues or `LazyInitializationException`.
- **Recommended Strategy**: Use explicit `@Getter` and `@Setter`. Rely on reference equality (`equals` / `hashCode` based solely on `id` if explicitly overridden, or default Java identity).

### 3.4 Patient Field Specifications

| Java Field | Database Column | Data Type | JPA Column Mapping Annotations | Constraints |
| :--- | :--- | :--- | :--- | :--- |
| `identifier` | `identifier` | `String` | `@Column(name = "identifier", length = 20, nullable = false, unique = true)` | UNIQUE, NOT NULL, Max 20 chars (CCCD / national ID) |
| `fullName` | `full_name` | `String` | `@Column(name = "full_name", length = 100, nullable = false)` | NOT NULL, Max 100 chars |
| `gender` | `gender` | `String` | `@Column(name = "gender", length = 10)` | Nullable, Max 10 chars ("male", "female", "other") |
| `birthDate` | `birth_date` | `LocalDate` | `@Column(name = "birth_date")` | Nullable, `java.time.LocalDate` |
| `phoneNumber` | `phone_number` | `String` | `@Column(name = "phone_number", length = 15)` | Nullable, Max 15 chars |

---

## 4. Proposed Source Code Blueprints for Implementation

Below are the exact code blueprints ready for implementer agents in M2.

### 4.1 `JpaAuditingConfig.java`
Path: `omnicare-emr-api/src/main/java/com/omnicare/emr/config/JpaAuditingConfig.java`
```java
package com.omnicare.emr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

### 4.2 `BaseEntity.java`
Path: `omnicare-emr-api/src/main/java/com/omnicare/emr/entity/BaseEntity.java`
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

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
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
    @Builder.Default
    private Long version = 0L;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;
}
```

### 4.3 `Patient.java`
Path: `omnicare-emr-api/src/main/java/com/omnicare/emr/entity/Patient.java`
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "patient")
@SQLDelete(sql = "UPDATE patient SET is_deleted = true WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
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

---

## 5. Verification & Acceptance Criteria Matrix

| Criterion | Requirement Reference | Expected Behavior / Artifact | Verification Command |
| :--- | :--- | :--- | :--- |
| **Spring Boot Build** | R2 | Maven project builds clean without errors | `mvn clean compile` inside `omnicare-emr-api` |
| **Table Schema Generation** | R3, TC-1.3 | Hibernate auto-creates `patient` table with 10 columns (`id`, `created_at`, `updated_at`, `version`, `is_deleted`, `identifier`, `full_name`, `gender`, `birth_date`, `phone_number`) | Direct PostgreSQL query: `SELECT column_name FROM information_schema.columns WHERE table_name = 'patient'` |
| **UUID PK Assignment** | R3, TC-2.1 | Auto-generated UUID v4 on `Patient` persist | Entity unit test / integration test saving `Patient` instance |
| **Audit Timestamps** | R3, TC-2.2 | `createdAt` and `updatedAt` non-null on persist | Integration test saving entity with `@EnableJpaAuditing` active |
| **Optimistic Locking** | R3, TC-4.3 | `version` initialized to `0`, incremented on concurrent edit | Concurrent edit test throwing `ObjectOptimisticLockingFailureException` |
| **Soft Delete Flag** | R3, TC-4.3 | `is_deleted` default `false`, soft delete updates flag without physical deletion | SQL query asserting `is_deleted = false` |

---

## 6. Summary & Recommendations for Milestone M2 Implementation
1. **Apply `@SuperBuilder`** on both `BaseEntity` and `Patient` to avoid Lombok inheritance issues.
2. **Include `@EnableJpaAuditing`** in `com.omnicare.emr.config.JpaAuditingConfig`.
3. **Use standard JPA `GenerationType.UUID`** for id field.
4. **Use `@SQLDelete` and `@SQLRestriction("is_deleted = false")`** for soft delete lifecycle control on `Patient`.
5. **Enforce unique constraint** on `identifier` (`@Column(unique = true)`).
