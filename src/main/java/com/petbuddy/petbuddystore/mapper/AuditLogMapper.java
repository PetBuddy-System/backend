package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.AuditLogResponse;
import com.petbuddy.petbuddystore.model.AuditLog;
import com.petbuddy.petbuddystore.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "performedBy", source = "performedBy", qualifiedByName = "getEmail")
    AuditLogResponse toResponse(AuditLog auditLog);

    List<AuditLogResponse> toResponseList(List<AuditLog> auditLogs);

    @Named("getEmail")
    default String getEmail(User user) {
        return user != null ? user.getEmail() : null;
    }
}