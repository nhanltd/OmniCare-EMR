package com.omnicare.emr.dto.mapper;

import com.omnicare.emr.dto.PatientRequestDto;
import com.omnicare.emr.dto.PatientResponseDto;
import com.omnicare.emr.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PatientMapper {

    Patient toEntity(PatientRequestDto requestDto);

    PatientResponseDto toDto(Patient entity);
}
