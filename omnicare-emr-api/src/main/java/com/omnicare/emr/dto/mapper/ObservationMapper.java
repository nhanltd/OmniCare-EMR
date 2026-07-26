package com.omnicare.emr.dto.mapper;

import com.omnicare.emr.dto.ObservationRequestDto;
import com.omnicare.emr.dto.ObservationResponseDto;
import com.omnicare.emr.entity.Observation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting between Observation entity and DTOs.
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ObservationMapper {

    Observation toEntity(ObservationRequestDto requestDto);

    @Mapping(source = "encounter.id", target = "encounterId")
    ObservationResponseDto toDto(Observation entity);
}
