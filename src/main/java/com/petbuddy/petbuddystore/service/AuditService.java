package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.AuditEntityType;
import com.petbuddy.petbuddystore.dto.request.AuditLogFilterRequest;
import com.petbuddy.petbuddystore.dto.request.AuditLogResponse;
import com.petbuddy.petbuddystore.model.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
public interface AuditService {

    // ============================================================
    // 1. LOG METHODS
    // ============================================================

    void logProductUpdate(Product oldProduct, Product newProduct, String reason, String note, User performedBy);

    void logPromotionUpdate(Promotion oldPromotion, Promotion newPromotion, String reason, String note, User performedBy);

    void logBatchUpdate(ProductBatch oldBatch, ProductBatch newBatch, String reason, String note, User performedBy);

    void logBatchStockAdjustment(ProductBatch batch, Integer oldStock, Integer newStock,
                                 String reason, String note, User performedBy);

    // ============================================================
    // 2. QUERY METHODS - TRẢ VỀ DTO
    // ============================================================

    // ⭐ Sửa return type từ List<AuditLog> thành List<AuditLogResponse>
    List<AuditLogResponse> filterAuditLogs(AuditLogFilterRequest filterRequest);

    List<AuditLogResponse> filterAuditLogs(AuditLogFilterRequest filterRequest, Pageable pageable);

    List<AuditLogResponse> filterAuditLogs(
            AuditEntityType entityType,
            UUID entityId,
            String action,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String performedBy,
            Pageable pageable
    );

    // ⭐ Sửa return type từ List<AuditLog> thành List<AuditLogResponse>
    List<AuditLogResponse> getAuditLogsByEntity(AuditEntityType entityType, UUID entityId);

    // ⭐ Sửa return type từ AuditLog thành AuditLogResponse
    AuditLogResponse getAuditLogById(UUID id);

    // ⭐ Sửa return type từ List<AuditLog> thành List<AuditLogResponse>
    List<AuditLogResponse> getRecentAuditLogs(int limit);
}
