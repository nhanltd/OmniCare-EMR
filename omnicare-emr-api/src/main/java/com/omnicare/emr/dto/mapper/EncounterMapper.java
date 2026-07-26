package com.omnicare.emr.dto.mapper;

import com.omnicare.emr.dto.EncounterRequestDto;
import com.omnicare.emr.dto.EncounterResponseDto;
import com.omnicare.emr.entity.Encounter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting between Encounter entity and DTOs.
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EncounterMapper {

    Encounter toEntity(EncounterRequestDto requestDto);

    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(source = "patient.fullName", target = "patientName")
    @Mapping(source = "practitioner.id", target = "practitionerId")
    @Mapping(source = "practitioner.fullName", target = "practitionerName")
    EncounterResponseDto toDto(Encounter entity);
}
