## 2026-07-24T14:51:53Z
You are Milestone M2 Implementation Worker.
Working directory: c:/Users/nhan/Workspace/OmniCare-EMR/.agents/worker_m2

Task: Implement Milestone M2 (Core Data Model & Persistence Configuration) for OmniCare EMR in omnicare-emr-api.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Detailed specifications to implement:
1. c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/entity/BaseEntity.java:
   - Abstract superclass annotated with @Getter, @Setter, @SuperBuilder, @NoArgsConstructor, @AllArgsConstructor, @MappedSuperclass, @EntityListeners(AuditingEntityListener.class).
   - id: UUID (@Id, @GeneratedValue(strategy = GenerationType.UUID), @Column(name = "id", updatable = false, nullable = false)).
   - createdAt: Instant (@CreatedDate, @Column(name = "created_at", nullable = false, updatable = false)).
   - updatedAt: Instant (@LastModifiedDate, @Column(name = "updated_at", nullable = false)).
   - version: Long (@Version, @Column(name = "version", nullable = false)).
   - isDeleted: boolean (@Builder.Default, @Column(name = "is_deleted", nullable = false) private boolean isDeleted = false;).

2. c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/entity/Patient.java:
   - Extends BaseEntity. Annotated with @Entity, @Table(name = "patient", uniqueConstraints = {@UniqueConstraint(name = "uk_patient_identifier", columnNames = {"identifier"})}), @Getter, @Setter, @SuperBuilder, @NoArgsConstructor, @AllArgsConstructor.
   - identifier: String (@Column(name = "identifier", nullable = false, unique = true, length = 20)).
   - fullName: String (@Column(name = "full_name", nullable = false, length = 100)).
   - gender: String (@Column(name = "gender", length = 10)).
   - birthDate: LocalDate (@Column(name = "birth_date")).
   - phoneNumber: String (@Column(name = "phone_number", length = 15)).

3. c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/config/JpaConfig.java:
   - @Configuration, @EnableJpaAuditing.

4. c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/resources/application.yml:
   - Configure PostgreSQL datasource (jdbc:postgresql://localhost:5432/omnicare_db, omnicare_user, omnicare_pass), jpa.hibernate.ddl-auto: update, show-sql: true.

5. Verification:
   - Run `mvn clean compile` in c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api directory using run_command and confirm compilation succeeds with exit code 0.

Write your report and handoff to c:/Users/nhan/Workspace/OmniCare-EMR/.agents/worker_m2/handoff.md and send a message back with your findings and build result.
