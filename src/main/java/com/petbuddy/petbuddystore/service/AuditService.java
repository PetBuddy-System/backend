package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.AuditEntityType;
import com.petbuddy.petbuddystore.dto.request.AuditLogFilterRequest;
import com.petbuddy.petbuddystore.dto.response.AuditLogResponse;
import com.petbuddy.petbuddystore.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
public interface AuditService {

    // ===== LOG METHODS =====
    void logProductCreate(Product product, String reason, String note, User performedBy);
    void logProductUpdate(Product oldProduct, Product newProduct, String reason, String note, User performedBy);

    void logPromotionCreate(Promotion promotion, String reason, String note, User performedBy);
    void logPromotionUpdate(Promotion oldPromotion, Promotion newPromotion, String reason, String note, User performedBy);

    void logBatchCreate(ProductBatch batch, String reason, String note, User performedBy);
    void logBatchUpdate(ProductBatch oldBatch, ProductBatch newBatch, String reason, String note, User performedBy);

    Page<AuditLogResponse> filterAuditLogs(AuditEntityType entityType, String entityCode, String action, LocalDateTime fromDate, LocalDateTime toDate, String performedBy, Pageable pageable);

    AuditLogResponse getAuditLogById(UUID id);
}