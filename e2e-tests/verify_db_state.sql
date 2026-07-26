-- =============================================================================
-- OmniCare EMR - Direct PostgreSQL Database State Verification Script
-- Author: Worker E2E
-- Description: Independent SQL assertions for table existence, BaseEntity audit columns,
--              constraints, default values, UUID PK generation, and UTF-8 data integrity.
-- =============================================================================

-- 1. Table Existence Check
SELECT EXISTS (
   SELECT FROM information_schema.tables 
   WHERE table_schema = 'public' 
   AND table_name = 'patient'
) AS patient_table_exists;

-- 2. Inspect Column Metadata (Data Types, Nullability, Defaults)
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'patient'
ORDER BY ordinal_position;

-- 3. Inspect Primary Key and Unique Constraints
SELECT 
    con.conname AS constraint_name, 
    con.contype AS constraint_type,
    pg_get_constraintdef(con.oid) AS constraint_definition
FROM pg_constraint con
JOIN pg_class rel ON rel.oid = con.conrelid
JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
WHERE nsp.nspname = 'public' 
  AND rel.relname = 'patient';

-- 4. Audit Column & Flag Verification on Active Patients
SELECT 
    id, 
    identifier, 
    full_name, 
    gender,
    birth_date,
    phone_number,
    created_at, 
    updated_at, 
    version, 
    is_deleted
FROM patient
WHERE is_deleted = false
ORDER BY created_at DESC;

-- 5. Duplicate CCCD Identifier Prevention Count Check
SELECT 
    identifier, 
    COUNT(*) as record_count
FROM patient
GROUP BY identifier
HAVING COUNT(*) > 1;
