package com.omnicare.emr.dto.mapper;

import com.omnicare.emr.dto.PractitionerRequestDto;
import com.omnicare.emr.dto.PractitionerResponseDto;
import com.omnicare.emr.entity.Practitioner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper converting between Practitioner entity and DTOs.
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PractitionerMapper {

    /**
     * Maps request DTO to a new Practitioner entity.
     */
    Practitioner toEntity(PractitionerRequestDto requestDto);

    /**
     * Maps Practitioner entity to response DTO.
     */
    PractitionerResponseDto toDto(Practitioner entity);

    /**
     * Updates an existing Practitioner entity in-place from a request DTO.
     */
    void updateEntityFromDto(PractitionerRequestDto requestDto, @MappingTarget Practitioner entity);
}
