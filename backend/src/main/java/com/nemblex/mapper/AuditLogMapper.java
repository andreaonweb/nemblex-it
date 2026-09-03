package com.nemblex.mapper;

import com.nemblex.dto.request.AuditLogRequest;
import com.nemblex.dto.response.AuditLogResponse;
import com.nemblex.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AuditLogMapper {

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "approvedByName", source = "approvedBy.name")
    AuditLogResponse toResponse(AuditLog auditLog);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "resultStatus", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    AuditLog toEntity(AuditLogRequest request);
}
