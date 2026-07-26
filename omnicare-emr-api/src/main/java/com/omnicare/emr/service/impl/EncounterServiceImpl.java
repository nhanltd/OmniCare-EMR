package com.omnicare.emr.service.impl;

import com.omnicare.emr.dto.DiagnosisRequestDto;
import com.omnicare.emr.dto.EncounterRequestDto;
import com.omnicare.emr.dto.EncounterResponseDto;
import com.omnicare.emr.dto.FinalizeEncounterRequestDto;
import com.omnicare.emr.dto.FinalizeEncounterResponseDto;
import com.omnicare.emr.dto.PrescriptionItemRequestDto;
import com.omnicare.emr.dto.mapper.DiagnosisMapper;
import com.omnicare.emr.dto.mapper.EncounterMapper;
import com.omnicare.emr.dto.mapper.PrescriptionItemMapper;
import com.omnicare.emr.entity.Diagnosis;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.entity.PrescriptionItem;
import com.omnicare.emr.exception.EncounterCancelledException;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.repository.DiagnosisRepository;
import com.omnicare.emr.repository.EncounterRepository;
import com.omnicare.emr.repository.PatientRepository;
import com.omnicare.emr.repository.PractitionerRepository;
import com.omnicare.emr.repository.PrescriptionItemRepository;
import com.omnicare.emr.service.EncounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link EncounterService}.
 */
@Service
@RequiredArgsConstructor
public class EncounterServiceImpl implements EncounterService {

    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final EncounterMapper encounterMapper;
    private final DiagnosisMapper diagnosisMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;

    @Override
    @Transactional
    public EncounterResponseDto createEncounter(EncounterRequestDto requestDto) {
        Patient patient = patientRepository.findByIdAndIsDeletedFalse(requestDto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + requestDto.getPatientId()));

        Practitioner practitioner = practitionerRepository.findByIdAndIsDeletedFalse(requestDto.getPractitionerId())
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found with ID: " + requestDto.getPractitionerId()));

        Encounter encounter = encounterMapper.toEntity(requestDto);
        encounter.setPatient(patient);
        encounter.setPractitioner(practitioner);

        if (requestDto.getStatus() != null) {
            encounter.setStatus(requestDto.getStatus());
        } else {
            encounter.setStatus(EncounterStatus.PLANNED);
        }

        Encounter savedEncounter = encounterRepository.save(encounter);
        return encounterMapper.toDto(savedEncounter);
    }

    @Override
    @Transactional(readOnly = true)
    public EncounterResponseDto getEncounterById(UUID id) {
        Encounter encounter = encounterRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + id));

        return encounterMapper.toDto(encounter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EncounterResponseDto> getAllEncounters() {
        return encounterRepository.findAllByIsDeletedFalse()
                .stream()
                .map(encounterMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EncounterResponseDto updateEncounterStatus(UUID id, EncounterStatus status) {
        Encounter encounter = encounterRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + id));

        encounter.setStatus(status);
        Encounter updatedEncounter = encounterRepository.save(encounter);
        return encounterMapper.toDto(updatedEncounter);
    }

    @Override
    @Transactional
    public FinalizeEncounterResponseDto finalizeEncounter(UUID encounterId, FinalizeEncounterRequestDto requestDto) {
        Encounter encounter = encounterRepository.findByIdAndIsDeletedFalse(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + encounterId));

        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new EncounterCancelledException("Cannot finalize a cancelled encounter");
        }
        if (encounter.getStatus() == EncounterStatus.FINISHED) {
            throw new IllegalStateException("Encounter is already finalized");
        }

        // Save Diagnoses FIRST
        List<Diagnosis> diagnoses = requestDto.getDiagnoses().stream()
                .map(dto -> {
                    Diagnosis d = diagnosisMapper.toEntity(dto);
                    d.setEncounter(encounter);
                    return d;
                })
                .toList();
        List<Diagnosis> savedDiagnoses = diagnosisRepository.saveAll(diagnoses);

        // Validate & Process Prescription Items SECOND
        List<PrescriptionItem> prescriptions = new ArrayList<>();
        for (PrescriptionItemRequestDto dto : requestDto.getPrescriptions()) {
            if (dto.getMedicationName() == null || dto.getMedicationName().isBlank()) {
                throw new IllegalArgumentException("Invalid prescription item: medication name is required");
            }
            if (dto.getDosage() == null || dto.getDosage() <= 0) {
                throw new IllegalArgumentException("Invalid prescription item: dosage must be greater than 0");
            }
            PrescriptionItem item = prescriptionItemMapper.toEntity(dto);
            item.setEncounter(encounter);
            prescriptions.add(item);
        }
        List<PrescriptionItem> savedPrescriptions = prescriptionItemRepository.saveAll(prescriptions);

        // Update Encounter status to FINISHED
        encounter.setStatus(EncounterStatus.FINISHED);
        Encounter updatedEncounter = encounterRepository.save(encounter);

        return FinalizeEncounterResponseDto.builder()
                .encounterId(updatedEncounter.getId())
                .status(updatedEncounter.getStatus())
                .updatedAt(updatedEncounter.getUpdatedAt())
                .diagnoses(savedDiagnoses.stream().map(diagnosisMapper::toDto).toList())
                .prescriptions(savedPrescriptions.stream().map(prescriptionItemMapper::toDto).toList())
                .build();
    }
}
