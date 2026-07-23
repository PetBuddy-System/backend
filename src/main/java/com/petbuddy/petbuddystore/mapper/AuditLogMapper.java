package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.AuditLogResponse;
import com.petbuddy.petbuddystore.model.AuditLog;
import com.petbuddy.petbuddystore.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "changes", ignore = true)
    @Mapping(target = "performedBy", source = "performedBy", qualifiedByName = "getEmail")
    @Mapping(target = "entityCode", source = "entityCode")
    AuditLogResponse toSummaryResponse(AuditLog auditLog);

    @Mapping(target = "performedBy", source = "performedBy", qualifiedByName = "getEmail")
    @Mapping(target = "entityCode", source = "entityCode")
    AuditLogResponse toDetailResponse(AuditLog auditLog);

    default List<AuditLogResponse> toSummaryResponseList(List<AuditLog> auditLogs) {
        if (auditLogs == null) return null;
        return auditLogs.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Named("getEmail")
    default String getEmail(User user) {
        return user != null ? user.getEmail() : null;
    }
}