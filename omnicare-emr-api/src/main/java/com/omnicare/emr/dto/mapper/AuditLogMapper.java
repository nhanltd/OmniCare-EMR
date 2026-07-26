package com.omnicare.emr.dto.mapper;

import com.omnicare.emr.dto.AuditLogResponseDto;
import com.omnicare.emr.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AuditLogMapper {

    AuditLogResponseDto toDto(AuditLog entity);
}
