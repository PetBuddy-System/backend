package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.AuditAction;
import com.petbuddy.petbuddystore.common.enums.AuditEntityType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AuditLogFilterRequest {
    private AuditEntityType entityType;
    private UUID entityId;
    private AuditAction action;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String performedBy;
}