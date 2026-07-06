package com.petbuddy.petbuddystore.dto.request;


import com.petbuddy.petbuddystore.common.enums.AuditAction;
import com.petbuddy.petbuddystore.common.enums.AuditEntityType;
import com.petbuddy.petbuddystore.model.AuditChange;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponse {
    private UUID id;
    private AuditEntityType entityType;
    private UUID entityId;
    private AuditAction action;
    private List<AuditChange> changes;
    private String reason;
    private String note;
    private String performedBy;  // Email của người thực hiện
    private LocalDateTime performedAt;
}