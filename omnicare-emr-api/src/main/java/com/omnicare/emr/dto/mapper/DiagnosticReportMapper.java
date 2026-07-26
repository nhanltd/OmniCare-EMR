package com.omnicare.emr.dto.mapper;

import com.omnicare.emr.dto.DiagnosticReportCreateRequestDto;
import com.omnicare.emr.dto.DiagnosticReportResponseDto;
import com.omnicare.emr.entity.DiagnosticReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface DiagnosticReportMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "encounter", ignore = true)
    @Mapping(target = "resultReceivedAt", ignore = true)
    @Mapping(target = "resultValue", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "referenceRange", ignore = true)
    @Mapping(target = "flag", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    DiagnosticReport toEntity(DiagnosticReportCreateRequestDto requestDto);

    @Mapping(source = "encounter.id", target = "encounterId")
    DiagnosticReportResponseDto toDto(DiagnosticReport entity);
}
