# Handoff Report: Milestone M2 Data Model & Persistence Requirements Strategy

**Author:** Explorer M2 Instance 2  
**Target Recipient:** Orchestrator / Implementer Agent  
**Date:** 2026-07-24  
**Working Directory:** `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_m2_2`  

---

## 1. Observation

### 1.1 Source Files & Current Project State
- **Root POM (`omnicare-emr-api/pom.xml`)**:
  - Spring Boot parent: `3.2.5`
  - Java version: `17`
  - Starter dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `postgresql`, `lombok`.
- **Application Properties (`omnicare-emr-api/src/main/resources/application.yml`)**:
  - Datasource URL: `jdbc:postgresql://localhost:5432/omnicare_db`
  - Username: `omnicare_user`, Password: `omnicare_pass`
  - Hibernate DDL auto strategy: `update`
  - Dialect: `org.hibernate.dialect.PostgreSQLDialect`
- **Application Class (`omnicare-emr-api/src/main/java/com/omnicare/emr/OmnicareApiApplication.java`)**:
  - Standard `@SpringBootApplication`. Does **not** include `@EnableJpaAuditing`.
- **Entity Package (`omnicare-emr-api/src/main/java/com/omnicare/emr/entity/`)**:
  - Contains only `package-info.java` from M1 package scaffolding.
- **Specification Documents**:
  - `knowledge/OMNICARE-EMR_Database_Design.md`: Dictates `BaseEntity` with 5 columns (`id`, `created_at`, `updated_at`, `version`, `is_deleted`) and `patient` table with 5 core domain columns (`identifier`, `full_name`, `gender`, `birth_date`, `phone_number`).
  - `TEST_READY.md` & `e2e-tests/test_tier1_infrastructure.py:26-29`: Requires exactly 10 columns on `patient` table in PostgreSQL: `id`, `created_at`, `updated_at`, `version`, `is_deleted`, `identifier`, `full_name`, `gender`, `birth_date`, `phone_number`.

---

## 2. Logic Chain

1. **Requirement Analysis (R3 & Database Design Spec)**:
   - `BaseEntity` must be an abstract base class (`@MappedSuperclass`) defining administrative audit fields (`id`, `createdAt`, `updatedAt`, `version`, `isDeleted`).
   - `Patient` must inherit from `BaseEntity` and map to table `patient`.
2. **Lombok `@SuperBuilder` Strategy**:
   - Standard `@Builder` fails when extending classes because it generates builder methods only for fields declared in the subclass.
   - Using `@SuperBuilder` on both `@MappedSuperclass BaseEntity` and `@Entity Patient` guarantees complete builder functionality across all 10 entity attributes (`BaseEntity` fields + `Patient` fields).
   - `@NoArgsConstructor` and `@AllArgsConstructor` are mandatory alongside `@SuperBuilder` to satisfy JPA default constructor requirements.
3. **Audit & Configuration Mechanics**:
   - Automatic setting of `@CreatedDate` (`createdAt`) and `@LastModifiedDate` (`updatedAt`) requires `@EntityListeners(AuditingEntityListener.class)` on `BaseEntity`.
   - Spring Data JPA Auditing will not populate timestamps unless `@EnableJpaAuditing` is declared. Creating a dedicated `@Configuration` class (`JpaAuditingConfig`) ensures clean separation and support for Mappings/Slice Testing.
4. **Primary Key & Soft Delete Mechanics**:
   - `GenerationType.UUID` (Jakarta Persistence 3.0 standard in Spring Boot 3) handles standard RFC 4122 UUID generation cleanly.
   - `@Version` on `Long version` handles Optimistic Locking (preventing concurrent write conflicts).
   - Soft Delete is enforced via `@SQLDelete(sql = "UPDATE patient SET is_deleted = true WHERE id = ? AND version = ?")` and `@SQLRestriction("is_deleted = false")`.
   - Field `isDeleted` uses `@Builder.Default private boolean isDeleted = false;` to guarantee `false` initialization during builder instantiation.

---

## 3. Caveats

1. **No Application Source Changes Made**: Explorer M2 Instance 2 is operating under a read-only investigation constraint. The source files (`JpaAuditingConfig.java`, `BaseEntity.java`, `Patient.java`) must be created by the M2 Implementer agent.
2. **Lombok Annotation Processor**: Ensure Maven compile uses Lombok `1.18.30+` annotation processor (already specified in `pom.xml` under `maven-compiler-plugin`).
3. **Database Liveness**: For `ddl-auto: update` to auto-generate the PostgreSQL schema during application boot, the PostgreSQL container must be running via `docker-compose up -d`.

---

## 4. Conclusion

The specification for Milestone M2 (Core Data Model & Persistence Configuration) is complete and fully validated against project requirements, database design specs, and E2E test constraints.

The implementer should create:
1. `com.omnicare.emr.config.JpaAuditingConfig` (`@Configuration`, `@EnableJpaAuditing`)
2. `com.omnicare.emr.entity.BaseEntity` (`@MappedSuperclass`, `@SuperBuilder`, `@EntityListeners(AuditingEntityListener.class)`, UUID PK, `createdAt`, `updatedAt`, `@Version version`, `isDeleted`)
3. `com.omnicare.emr.entity.Patient` (`@Entity`, `@Table(name = "patient")`, `@SuperBuilder`, `@SQLDelete`, `@SQLRestriction("is_deleted = false")`, `identifier` unique constraint, `fullName`, `gender`, `birthDate`, `phoneNumber`).

Full source code blueprints and verification criteria are provided in `analysis.md`.

---

## 5. Verification Method

To verify the implementation of Milestone M2 once completed by the Implementer:

### 1. Source Compilation & Build Check
```powershell
cd c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api
mvn clean compile
```
*Expected Result:* BUILD SUCCESS without Lombok or JPA mapping errors.

### 2. Database Schema Auto-Generation Verification
1. Start PostgreSQL container:
   ```powershell
   cd c:/Users/nhan/Workspace/OmniCare-EMR
   docker compose up -d
   ```
2. Run Spring Boot application:
   ```powershell
   cd c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api
   mvn spring-boot:run
   ```
3. Run Tier 1 Schema Check in E2E tests or psql:
   ```powershell
   pytest e2e-tests/test_tier1_infrastructure.py -k test_tier1_db_schema -v
   ```
   Or execute raw SQL:
   ```sql
   SELECT column_name, data_type 
   FROM information_schema.columns 
   WHERE table_name = 'patient' AND table_schema = 'public';
   ```
*Expected Result:* All 10 columns (`id`, `created_at`, `updated_at`, `version`, `is_deleted`, `identifier`, `full_name`, `gender`, `birth_date`, `phone_number`) are present in `patient` table.
