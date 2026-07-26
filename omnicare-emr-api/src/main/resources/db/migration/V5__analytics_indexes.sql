-- Migration V5: Analytics Indexes
CREATE INDEX IF NOT EXISTS idx_diagnostic_report_tat ON diagnostic_report(ordered_at, result_received_at);
CREATE INDEX IF NOT EXISTS idx_encounter_date ON encounter(encounter_date);
CREATE INDEX IF NOT EXISTS idx_patient_identifier ON patient(identifier);

