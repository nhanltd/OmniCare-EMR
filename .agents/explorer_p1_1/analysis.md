# Analysis and Proposed Flyway Migration Script

## 1. Executive Summary & Objective
The goal of this task is to analyze the existing database configuration (`application.yml`) and database schema (`V1__init_schema.sql`), and design the Flyway migration script `V2__create_practitioner_table_and_seed.sql` for the `practitioner` table along with mock seed data in the OmniCare EMR project.

---

## 2. Examination of Existing Files

### 2.1 `application.yml`
- **Database Engine**: PostgreSQL (`jdbc:postgresql://localhost:5432/omnicare_db`)
- **Hibernate Dialect**: `org.hibernate.dialect.PostgreSQLDialect`
- **DDL Strategy**: `hibernate.ddl-auto: validate` (schema strictly managed by Flyway migrations)
- **Flyway**: `flyway.enabled: true`, `baseline-on-migrate: true`

### 2.2 `V1__init_schema.sql` (Existing `patient` table)
```sql
CREATE TABLE patient (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    identifier VARCHAR(20) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    gender VARCHAR(10),
    birth_date DATE,
    phone_number VARCHAR(15),
    CONSTRAINT uk_patient_identifier UNIQUE (identifier)
);
```

### 2.3 Conventions Observed
- Table names: `snake_case`, singular form (`patient`, `practitioner`).
- Primary Key: `id UUID PRIMARY KEY`.
- Audit columns:
  - `created_at TIMESTAMP WITH TIME ZONE NOT NULL`
  - `updated_at TIMESTAMP WITH TIME ZONE NOT NULL`
  - `version BIGINT NOT NULL DEFAULT 0`
  - `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`
- Unique Constraints: Explicitly named using pattern `CONSTRAINT uk_<table_name>_<column_name> UNIQUE (<column_name>)`.

---

## 3. Practitioner DDL Specification

The `practitioner` table represents healthcare practitioners (doctors, nurses, technicians) and extends `BaseEntity`.

| Column | Data Type | Nullable | Default | Description / Constraints |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | NO | — | PRIMARY KEY |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | NO | — | Audit creation timestamp |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | NO | — | Audit update timestamp |
| `version` | `BIGINT` | NO | `0` | Optimistic locking version |
| `is_deleted` | `BOOLEAN` | NO | `FALSE` | Soft delete flag |
| `practitioner_code` | `VARCHAR(50)` | NO | — | Unique employee/license code (`uk_practitioner_code`) |
| `full_name` | `VARCHAR(100)` | NO | — | Practitioner full name |
| `specialty` | `VARCHAR(50)` | NO | — | Medical specialty (e.g. CARDIOLOGY) |
| `practitioner_type` | `VARCHAR(20)` | NO | — | Category (`DOCTOR`, `NURSE`, `TECHNICIAN`) |
| `phone` | `VARCHAR(20)` | YES | — | Contact phone number |
| `email` | `VARCHAR(100)` | YES | — | Contact email address |

---

## 4. Seed Data Design

We define 5 realistic mock records covering different roles (`DOCTOR`, `NURSE`, `TECHNICIAN`) and specialties (`CARDIOLOGY`, `PEDIATRICS`, `GENERAL_SURGERY`, `DERMATOLOGY`, `ORTHOPEDICS`).

| ID | Code | Full Name | Specialty | Type | Phone | Email |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `11111111-1111-1111-1111-111111111111` | `PRAC-001` | Dr. Sarah Connor | CARDIOLOGY | DOCTOR | +1-555-0101 | sarah.connor@omnicare.com |
| `22222222-2222-2222-2222-222222222222` | `PRAC-002` | Dr. Marcus Vance | PEDIATRICS | DOCTOR | +1-555-0102 | marcus.vance@omnicare.com |
| `33333333-3333-3333-3333-333333333333` | `PRAC-003` | Elena Rostova, RN | GENERAL_SURGERY | NURSE | +1-555-0103 | elena.rostova@omnicare.com |
| `44444444-4444-4444-4444-444444444444` | `PRAC-004` | Dr. Robert Chen | DERMATOLOGY | DOCTOR | +1-555-0104 | robert.chen@omnicare.com |
| `55555555-5555-5555-5555-555555555555` | `PRAC-005` | David Miller | ORTHOPEDICS | TECHNICIAN | +1-555-0105 | david.miller@omnicare.com |

All rows use fixed UTC timestamps (`2026-01-15 08:00:00+00`), `version = 0`, and `is_deleted = FALSE`.

---

## 5. Exact Proposed SQL Migration File (`V2__create_practitioner_table_and_seed.sql`)

Target File Path: `omnicare-emr-api/src/main/resources/db/migration/V2__create_practitioner_table_and_seed.sql`

```sql
CREATE TABLE practitioner (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    practitioner_code VARCHAR(50) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    specialty VARCHAR(50) NOT NULL,
    practitioner_type VARCHAR(20) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    CONSTRAINT uk_practitioner_code UNIQUE (practitioner_code)
);

INSERT INTO practitioner (
    id,
    created_at,
    updated_at,
    version,
    is_deleted,
    practitioner_code,
    full_name,
    specialty,
    practitioner_type,
    phone,
    email
) VALUES
('11111111-1111-1111-1111-111111111111', '2026-01-15 08:00:00+00', '2026-01-15 08:00:00+00', 0, FALSE, 'PRAC-001', 'Dr. Sarah Connor', 'CARDIOLOGY', 'DOCTOR', '+1-555-0101', 'sarah.connor@omnicare.com'),
('22222222-2222-2222-2222-222222222222', '2026-01-15 08:00:00+00', '2026-01-15 08:00:00+00', 0, FALSE, 'PRAC-002', 'Dr. Marcus Vance', 'PEDIATRICS', 'DOCTOR', '+1-555-0102', 'marcus.vance@omnicare.com'),
('33333333-3333-3333-3333-333333333333', '2026-01-15 08:00:00+00', '2026-01-15 08:00:00+00', 0, FALSE, 'PRAC-003', 'Elena Rostova, RN', 'GENERAL_SURGERY', 'NURSE', '+1-555-0103', 'elena.rostova@omnicare.com'),
('44444444-4444-4444-4444-444444444444', '2026-01-15 08:00:00+00', '2026-01-15 08:00:00+00', 0, FALSE, 'PRAC-004', 'Dr. Robert Chen', 'DERMATOLOGY', 'DOCTOR', '+1-555-0104', 'robert.chen@omnicare.com'),
('55555555-5555-5555-5555-555555555555', '2026-01-15 08:00:00+00', '2026-01-15 08:00:00+00', 0, FALSE, 'PRAC-005', 'David Miller', 'ORTHOPEDICS', 'TECHNICIAN', '+1-555-0105', 'david.miller@omnicare.com');
```
