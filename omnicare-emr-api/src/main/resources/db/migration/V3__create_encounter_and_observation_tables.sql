-- Table: encounter
CREATE TABLE encounter (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id UUID NOT NULL,
    practitioner_id UUID NOT NULL,
    encounter_date TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(512),
    CONSTRAINT fk_encounter_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_encounter_practitioner FOREIGN KEY (practitioner_id) REFERENCES practitioner(id)
);

-- Indexes for encounter table
CREATE INDEX idx_encounter_patient_id ON encounter(patient_id);
CREATE INDEX idx_encounter_practitioner_id ON encounter(practitioner_id);
CREATE INDEX idx_encounter_status ON encounter(status);

-- Table: observation
CREATE TABLE observation (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    encounter_id UUID NOT NULL,
    value_json JSONB NOT NULL,
    CONSTRAINT fk_observation_encounter FOREIGN KEY (encounter_id) REFERENCES encounter(id)
);

-- Indexes for observation table
CREATE INDEX idx_observation_encounter_id ON observation(encounter_id);
CREATE INDEX idx_observation_value_json ON observation USING gin(value_json);
