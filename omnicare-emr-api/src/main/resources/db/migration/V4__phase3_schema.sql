-- Migration V4: Phase 3 Schema (Diagnostic Report, Diagnosis, Prescription Item, Audit Log)

-- Table: diagnostic_report
CREATE TABLE diagnostic_report (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    encounter_id UUID NOT NULL,
    ordered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    result_received_at TIMESTAMP WITH TIME ZONE,
    test_code VARCHAR(50) NOT NULL,
    test_name VARCHAR(100) NOT NULL,
    result_value VARCHAR(255),
    unit VARCHAR(50),
    reference_range VARCHAR(100),
    flag VARCHAR(20),
    status VARCHAR(32) NOT NULL,
    CONSTRAINT fk_diagnostic_report_encounter FOREIGN KEY (encounter_id) REFERENCES encounter(id)
);

CREATE INDEX idx_diagnostic_report_encounter_id ON diagnostic_report(encounter_id);
CREATE INDEX idx_diagnostic_report_status ON diagnostic_report(status);
CREATE INDEX idx_diagnostic_report_test_code ON diagnostic_report(test_code);

-- Table: diagnosis
CREATE TABLE diagnosis (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    encounter_id UUID NOT NULL,
    icd10_code VARCHAR(16) NOT NULL,
    description VARCHAR(512) NOT NULL,
    CONSTRAINT fk_diagnosis_encounter FOREIGN KEY (encounter_id) REFERENCES encounter(id)
);

CREATE INDEX idx_diagnosis_encounter_id ON diagnosis(encounter_id);
CREATE INDEX idx_diagnosis_icd10_code ON diagnosis(icd10_code);

-- Table: prescription_item
CREATE TABLE prescription_item (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    encounter_id UUID NOT NULL,
    medication_name VARCHAR(255) NOT NULL,
    dosage DOUBLE PRECISION NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    duration VARCHAR(100) NOT NULL,
    CONSTRAINT fk_prescription_item_encounter FOREIGN KEY (encounter_id) REFERENCES encounter(id)
);

CREATE INDEX idx_prescription_item_encounter_id ON prescription_item(encounter_id);

-- Table: audit_log
CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    entity_id UUID NOT NULL,
    old_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    action VARCHAR(64) NOT NULL
);

CREATE INDEX idx_audit_log_entity_id ON audit_log(entity_id);
CREATE INDEX idx_audit_log_changed_at ON audit_log(changed_at);
CREATE INDEX idx_audit_log_action ON audit_log(action);
