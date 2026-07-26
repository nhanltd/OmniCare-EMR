package com.omnicare.emr.dto.mapper;

import com.omnicare.emr.dto.PrescriptionItemRequestDto;
import com.omnicare.emr.dto.PrescriptionItemResponseDto;
import com.omnicare.emr.entity.PrescriptionItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PrescriptionItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "encounter", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    PrescriptionItem toEntity(PrescriptionItemRequestDto requestDto);

    @Mapping(source = "encounter.id", target = "encounterId")
    PrescriptionItemResponseDto toDto(PrescriptionItem entity);
}
