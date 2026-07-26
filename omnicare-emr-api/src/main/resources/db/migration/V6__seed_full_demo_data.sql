-- =================================================================
-- Flyway Migration V6: Rich Seed Data for EMR Demo & API Testing
-- =================================================================

-- 1. Seed Patients
INSERT INTO patient (id, created_at, updated_at, version, is_deleted, identifier, full_name, gender, birth_date, phone_number) VALUES
('a0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', 0, FALSE, '079090001111', 'Pham Van An', 'male', '1985-04-12', '+84901111222'),
('a0000000-0000-0000-0000-000000000002', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', 0, FALSE, '079090002222', 'Nguyen Thi Binh', 'female', '1990-08-25', '+84902222333'),
('a0000000-0000-0000-0000-000000000003', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', 0, FALSE, '079090003333', 'Tran Quoc Cuong', 'male', '1978-11-03', '+84903333444'),
('a0000000-0000-0000-0000-000000000004', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', 0, FALSE, '079090004444', 'Le My Duyen', 'female', '1995-02-14', '+84904444555'),
('a0000000-0000-0000-0000-000000000005', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', 0, FALSE, '079090005555', 'Hoang Anh Tuan', 'male', '2001-06-30', '+84905555666')
ON CONFLICT (id) DO NOTHING;

-- 2. Seed Encounters
INSERT INTO encounter (id, created_at, updated_at, version, is_deleted, patient_id, practitioner_id, encounter_date, status, reason) VALUES
('e0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', 0, FALSE, 'a0000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', NOW() - INTERVAL '2 days', 'FINISHED', 'Routine Diabetes and General Checkup'),
('e0000000-0000-0000-0000-000000000002', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', 0, FALSE, 'a0000000-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', NOW() - INTERVAL '1 day', 'FINISHED', 'Hypertension and Headache Evaluation'),
('e0000000-0000-0000-0000-000000000003', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours', 0, FALSE, 'a0000000-0000-0000-0000-000000000003', '44444444-4444-4444-4444-444444444444', NOW() - INTERVAL '5 hours', 'IN_PROGRESS', 'Dermatology Rash Examination'),
('e0000000-0000-0000-0000-000000000004', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour', 0, FALSE, 'a0000000-0000-0000-0000-000000000004', '55555555-5555-5555-5555-555555555555', NOW() - INTERVAL '1 hour', 'PLANNED', 'Orthopedics Followup Check'),
('e0000000-0000-0000-0000-000000000005', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', 0, FALSE, 'a0000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', NOW() - INTERVAL '3 days', 'CANCELLED', 'Patient Cancelled Appointment')
ON CONFLICT (id) DO NOTHING;

-- 3. Seed Observations (Vitals in JSONB)
INSERT INTO observation (id, created_at, updated_at, version, is_deleted, encounter_id, value_json) VALUES
('b0000000-0000-0000-0000-000000000001', NOW(), NOW(), 0, FALSE, 'e0000000-0000-0000-0000-000000000001', '{"bloodPressure": "130/85", "heartRate": 78, "temperature": 36.8, "spo2": 98}'),
('b0000000-0000-0000-0000-000000000002', NOW(), NOW(), 0, FALSE, 'e0000000-0000-0000-0000-000000000002', '{"bloodPressure": "145/95", "heartRate": 85, "temperature": 37.0, "spo2": 97}'),
('b0000000-0000-0000-0000-000000000003', NOW(), NOW(), 0, FALSE, 'e0000000-0000-0000-0000-000000000003', '{"bloodPressure": "120/80", "heartRate": 72, "temperature": 36.6, "spo2": 99}')
ON CONFLICT (id) DO NOTHING;

-- 4. Seed Diagnostic Reports (LIS Lab Results)
INSERT INTO diagnostic_report (id, created_at, updated_at, version, is_deleted, encounter_id, test_code, test_name, result_value, unit, reference_range, flag, status, ordered_at, result_received_at) VALUES
('c0000000-0000-0000-0000-000000000001', NOW(), NOW(), 0, FALSE, 'e0000000-0000-0000-0000-000000000001', 'GLU-01', 'Fasting Blood Glucose', '7.2', 'mmol/L', '3.9 - 6.4', 'HIGH', 'FINAL', NOW() - INTERVAL '2 days' - INTERVAL '90 minutes', NOW() - INTERVAL '2 days' - INTERVAL '30 minutes'),
('c0000000-0000-0000-0000-000000000002', NOW(), NOW(), 0, FALSE, 'e0000000-0000-0000-0000-000000000002', 'LIP-01', 'Lipid Panel', '6.5', 'mmol/L', '< 5.2', 'HIGH', 'FINAL', NOW() - INTERVAL '1 day' - INTERVAL '60 minutes', NOW() - INTERVAL '1 day' - INTERVAL '15 minutes')
ON CONFLICT (id) DO NOTHING;

-- 5. Seed Diagnoses (ICD-10)
INSERT INTO diagnosis (id, created_at, updated_at, version, is_deleted, encounter_id, icd10_code, description) VALUES
('d0000000-0000-0000-0000-000000000001', NOW(), NOW(), 0, FALSE, 'e0000000-0000-0000-0000-000000000001', 'E11.9', 'Type 2 diabetes mellitus without complications'),
('d0000000-0000-0000-0000-000000000002', NOW(), NOW(), 0, FALSE, 'e0000000-0000-0000-0000-000000000002', 'I10', 'Essential primary hypertension')
ON CONFLICT (id) DO NOTHING;

-- 6. Seed Prescription Items
INSERT INTO prescription_item (id, created_at, updated_at, version, is_deleted, encounter_id, medication_name, dosage, frequency, duration) VALUES
('f0000000-0000-0000-0000-000000000001', NOW(), NOW(), 0, FALSE, 'e0000000-0000-0000-0000-000000000001', 'Metformin 500mg', 500.0, '2 tablets daily', '30 days'),
('f0000000-0000-0000-0000-000000000002', NOW(), NOW(), 0, FALSE, 'e0000000-0000-0000-0000-000000000002', 'Lisinopril 10mg', 10.0, '1 tablet daily', '30 days')
ON CONFLICT (id) DO NOTHING;

-- 7. Seed Audit Logs
INSERT INTO audit_log (id, created_at, updated_at, version, is_deleted, entity_id, old_status, new_status, changed_at, action) VALUES
('f0000000-0000-0000-0000-000000000010', NOW(), NOW(), 0, FALSE, 'e0000000-0000-0000-0000-000000000001', 'IN_PROGRESS', 'FINISHED', NOW() - INTERVAL '2 days', 'Finalized clinical exam and issued prescription'),
('f0000000-0000-0000-0000-000000000020', NOW(), NOW(), 0, FALSE, 'e0000000-0000-0000-0000-000000000002', 'IN_PROGRESS', 'FINISHED', NOW() - INTERVAL '1 day', 'Finalized clinical exam and issued prescription')
ON CONFLICT (id) DO NOTHING;
