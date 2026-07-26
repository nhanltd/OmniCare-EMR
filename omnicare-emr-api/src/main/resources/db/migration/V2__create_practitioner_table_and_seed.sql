CREATE TABLE practitioner (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    practitioner_code VARCHAR(50) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    specialty VARCHAR(100) NOT NULL,
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
