package com.omnicare.emr.dto.mapper;

import com.omnicare.emr.dto.DiagnosisRequestDto;
import com.omnicare.emr.dto.DiagnosisResponseDto;
import com.omnicare.emr.entity.Diagnosis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface DiagnosisMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "encounter", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Diagnosis toEntity(DiagnosisRequestDto requestDto);

    @Mapping(source = "encounter.id", target = "encounterId")
    DiagnosisResponseDto toDto(Diagnosis entity);
}
