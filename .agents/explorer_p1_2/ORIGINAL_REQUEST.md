## 2026-07-25T05:40:21Z
You are Explorer 2 (Domain & Repository Architecture Analyst) working in `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_p1_2`.

Your task is to analyze the domain layer and repository structure in `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/entity/` (specifically `BaseEntity.java` and `Patient.java`) and `repository/` (`PatientRepository.java`).

Design the implementation for:
1. `PractitionerType` Enum (`DOCTOR`, `NURSE`, `TECHNICIAN`).
2. `Practitioner` Entity inheriting from `BaseEntity`:
   - `@Entity`, `@Table(name = "practitioner")`
   - Fields: `practitionerCode` (unique, nullable=false), `fullName` (nullable=false), `specialty` (nullable=false), `practitionerType` (`@Enumerated(EnumType.STRING)`), `phone`, `email`.
   - Lombok annotations (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@SuperBuilder`, `@EqualsAndHashCode(callSuper = true)`).
3. `PractitionerRepository` interface extending `JpaRepository<Practitioner, UUID>`:
   - `boolean existsByPractitionerCode(String practitionerCode);`
   - `boolean existsByPractitionerCodeAndIdNot(String practitionerCode, UUID id);`
   - `Optional<Practitioner> findByIdAndIsDeletedFalse(UUID id);`
   - `List<Practitioner> findAllByIsDeletedFalse();`

Read existing entity and repository classes to maintain identical coding style and JPA mapping rules. Write your analysis to `.agents/explorer_p1_2/analysis.md` and write your handoff report to `.agents/explorer_p1_2/handoff.md`. Communicate your results back to the parent agent via `send_message`.
