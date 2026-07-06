package com.petbuddy.petbuddystore.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petbuddy.petbuddystore.common.enums.AuditAction;
import com.petbuddy.petbuddystore.common.enums.AuditEntityType;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.AuditLogFilterRequest;
import com.petbuddy.petbuddystore.dto.request.AuditLogResponse;
import com.petbuddy.petbuddystore.dto.response.PromotionDetailResponse;
import com.petbuddy.petbuddystore.mapper.AuditLogMapper;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.AuditLogRepository;
import com.petbuddy.petbuddystore.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional
    public void logProductUpdate(Product oldProduct, Product newProduct, String reason, String note, User performedBy) {
        List<AuditChange> changes = buildProductChanges(oldProduct, newProduct);

        if (changes.isEmpty()) {log.info("No changes detected for Product ID: {}", oldProduct.getProductId());
            return;
        }
        AuditLog auditLog = buildAuditLog(AuditEntityType.PRODUCT, oldProduct.getProductId(), changes, reason, note, performedBy);
        auditLogRepository.save(auditLog);
        log.info("Audit log created for Product ID: {}", oldProduct.getProductId());
    }

    @Override
    @Transactional
    public void logPromotionUpdate(Promotion oldPromotion, Promotion newPromotion, String reason, String note, User performedBy) {
        List<AuditChange> changes = buildPromotionChanges(oldPromotion, newPromotion);

        if (changes.isEmpty()) {log.info("No changes detected for Promotion ID: {}", oldPromotion.getPromotionId());
            return;
        }
        AuditLog auditLog = buildAuditLog(AuditEntityType.PROMOTION, oldPromotion.getPromotionId(), changes, reason, note, performedBy);
        auditLogRepository.save(auditLog);
        log.info("Audit log created for Promotion ID: {}", oldPromotion.getPromotionId());
    }

    @Override
    @Transactional
    public void logBatchUpdate(ProductBatch oldBatch, ProductBatch newBatch, String reason, String note, User performedBy) {
        List<AuditChange> changes = buildBatchChanges(oldBatch, newBatch);

        if (changes.isEmpty()) {log.info("No changes detected for Batch ID: {}", oldBatch.getBatchId());
            return;
        }
        AuditLog auditLog = buildAuditLog(AuditEntityType.BATCH, oldBatch.getBatchId(), changes, reason, note, performedBy);
        auditLogRepository.save(auditLog);
        log.info("Audit log created for Batch ID: {}", oldBatch.getBatchId());
    }

    @Override
    @Transactional
    public void logBatchStockAdjustment(ProductBatch batch, Integer oldStock, Integer newStock, String reason, String note, User performedBy) {
        List<AuditChange> changes = new ArrayList<>();
        changes.add(new AuditChange("stockQuantity", oldStock != null ? oldStock.toString() : null, newStock != null ? newStock.toString() : null));
        AuditLog auditLog = buildAuditLog(AuditEntityType.BATCH, batch.getBatchId(), changes, reason, note, performedBy);
        auditLogRepository.save(auditLog);
        log.info("Stock adjustment logged for Batch ID: {}, {} → {}", batch.getBatchId(), oldStock, newStock);
    }

    @Override
    public List<AuditLogResponse> filterAuditLogs(AuditLogFilterRequest filterRequest) {
        Specification<AuditLog> spec = buildFilterSpecification(filterRequest);
        List<AuditLog> auditLogs = auditLogRepository.findAll(spec);
        return auditLogMapper.toResponseList(auditLogs);
    }

    @Override
    public List<AuditLogResponse> filterAuditLogs(AuditLogFilterRequest filterRequest, Pageable pageable) {
        Specification<AuditLog> spec = buildFilterSpecification(filterRequest);

        try {
            // ⭐ Thử lấy dữ liệu mà không dùng sort
            Pageable pageableWithoutSort = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );
            List<AuditLog> auditLogs = auditLogRepository.findAll(spec, pageableWithoutSort).getContent();
            return auditLogMapper.toResponseList(auditLogs);
        } catch (Exception e) {
            log.error("Error filtering audit logs: {}", e.getMessage());
            // Fallback: lấy tất cả không phân trang
            List<AuditLog> auditLogs = auditLogRepository.findAll(spec);
            return auditLogMapper.toResponseList(auditLogs);
        }
    }

    @Override
    public List<AuditLogResponse> filterAuditLogs(AuditEntityType entityType, UUID entityId, String action, LocalDateTime fromDate, LocalDateTime toDate, String performedBy, Pageable pageable
    ) {
        AuditLogFilterRequest filterRequest = new AuditLogFilterRequest();
        filterRequest.setEntityType(entityType);
        filterRequest.setEntityId(entityId);
        filterRequest.setFromDate(fromDate);
        filterRequest.setToDate(toDate);
        filterRequest.setPerformedBy(performedBy);

        if (action != null) {
            try {
                filterRequest.setAction(AuditAction.valueOf(action.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid action value: {}", action);
            }
        }
        return filterAuditLogs(filterRequest, pageable);
    }

    @Override
    public List<AuditLogResponse> getAuditLogsByEntity(AuditEntityType entityType, UUID entityId) {
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByPerformedAtDesc(entityType, entityId);
        return auditLogMapper.toResponseList(auditLogs);
    }

    @Override
    public AuditLogResponse getAuditLogById(UUID id) {
        AuditLog auditLog = auditLogRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.AUDIT_LOG_NOT_FOUND));
        return auditLogMapper.toResponse(auditLog);
    }

    @Override
    public List<AuditLogResponse> getRecentAuditLogs(int limit) {
        List<AuditLog> auditLogs = auditLogRepository.findTopNByOrderByPerformedAtDesc(limit);
        return auditLogMapper.toResponseList(auditLogs);
    }


    private List<AuditChange> buildProductChanges(Product oldProduct, Product newProduct) {
        List<AuditChange> changes = new ArrayList<>();

        if (!Objects.equals(oldProduct.getName(), newProduct.getName())) {
            changes.add(new AuditChange("name", oldProduct.getName(), newProduct.getName()));
        }

        if (!Objects.equals(oldProduct.getDescription(), newProduct.getDescription())) {
            changes.add(new AuditChange("description", oldProduct.getDescription(), newProduct.getDescription()));
        }

        if (!Objects.equals(oldProduct.getIngredients(), newProduct.getIngredients())) {
            changes.add(new AuditChange("ingredients", oldProduct.getIngredients(), newProduct.getIngredients()));
        }

        if (!Objects.equals(oldProduct.getUsageInstructions(), newProduct.getUsageInstructions())) {
            changes.add(new AuditChange("usageInstructions", oldProduct.getUsageInstructions(), newProduct.getUsageInstructions()));
        }

        if (!Objects.equals(oldProduct.getSalePrice(), newProduct.getSalePrice())) {
            changes.add(new AuditChange("salePrice",
                    oldProduct.getSalePrice() != null ? oldProduct.getSalePrice().toString() : null,
                    newProduct.getSalePrice() != null ? newProduct.getSalePrice().toString() : null
            ));
        }

        if (!Objects.equals(oldProduct.getUnit(), newProduct.getUnit())) {
            changes.add(new AuditChange("unit",
                    oldProduct.getUnit() != null ? oldProduct.getUnit().name() : null,
                    newProduct.getUnit() != null ? newProduct.getUnit().name() : null
            ));
        }

        if (!Objects.equals(oldProduct.getBrandName(), newProduct.getBrandName())) {
            changes.add(new AuditChange("brandName", oldProduct.getBrandName(), newProduct.getBrandName()));
        }

        if (!Objects.equals(oldProduct.getStatus(), newProduct.getStatus())) {
            changes.add(new AuditChange("status",
                    oldProduct.getStatus() != null ? oldProduct.getStatus().name() : null,
                    newProduct.getStatus() != null ? newProduct.getStatus().name() : null
            ));
        }

        if (!Objects.equals(oldProduct.getDeletedAt(), newProduct.getDeletedAt())) {
            changes.add(new AuditChange("deletedAt",
                    oldProduct.getDeletedAt() != null ? oldProduct.getDeletedAt().toString() : null,
                    newProduct.getDeletedAt() != null ? newProduct.getDeletedAt().toString() : null
            ));
        }
        return changes;
    }

    private List<AuditChange> buildPromotionChanges(Promotion oldPromotion, Promotion newPromotion) {
        List<AuditChange> changes = new ArrayList<>();

        // Thông tin chương trình
        if (!Objects.equals(oldPromotion.getName(), newPromotion.getName())) {
            changes.add(new AuditChange("name", oldPromotion.getName(), newPromotion.getName()));
        }

        if (!Objects.equals(oldPromotion.getDescription(), newPromotion.getDescription())) {
            changes.add(new AuditChange("description", oldPromotion.getDescription(), newPromotion.getDescription()));
        }

        if (!Objects.equals(oldPromotion.getStartDate(), newPromotion.getStartDate())) {
            changes.add(new AuditChange("startDate",
                    oldPromotion.getStartDate() != null ? oldPromotion.getStartDate().toString() : null,
                    newPromotion.getStartDate() != null ? newPromotion.getStartDate().toString() : null
            ));
        }

        if (!Objects.equals(oldPromotion.getEndDate(), newPromotion.getEndDate())) {
            changes.add(new AuditChange("endDate",
                    oldPromotion.getEndDate() != null ? oldPromotion.getEndDate().toString() : null,
                    newPromotion.getEndDate() != null ? newPromotion.getEndDate().toString() : null
            ));
        }

        if (!Objects.equals(oldPromotion.getStatus(), newPromotion.getStatus())) {
            changes.add(new AuditChange("status",
                    oldPromotion.getStatus() != null ? oldPromotion.getStatus().name() : null,
                    newPromotion.getStatus() != null ? newPromotion.getStatus().name() : null
            ));
        }

        if (!Objects.equals(oldPromotion.getDeletedAt(), newPromotion.getDeletedAt())) {
            changes.add(new AuditChange("deletedAt",
                    oldPromotion.getDeletedAt() != null ? oldPromotion.getDeletedAt().toString() : null,
                    newPromotion.getDeletedAt() != null ? newPromotion.getDeletedAt().toString() : null
            ));
        }

        List<AuditChange> detailChanges = buildPromotionDetailChanges(
                oldPromotion.getPromotionDetails(),
                newPromotion.getPromotionDetails()
        );
        changes.addAll(detailChanges);
        return changes;
    }

    private List<AuditChange> buildPromotionDetailChanges(List<PromotionDetail> oldDetails, List<PromotionDetail> newDetails) {
        List<AuditChange> changes = new ArrayList<>();

        Map<UUID, PromotionDetail> oldMap = oldDetails != null ? oldDetails.stream()
                .collect(Collectors.toMap(
                        d -> d.getProduct().getProductId(),
                        d -> d,
                        (existing, replacement) -> existing
                )) : new HashMap<>();

        Map<UUID, PromotionDetail> newMap = newDetails != null ? newDetails.stream()
                .collect(Collectors.toMap(
                        d -> d.getProduct().getProductId(),
                        d -> d,
                        (existing, replacement) -> existing
                )) : new HashMap<>();

        for (UUID productId : oldMap.keySet()) {
            if (!newMap.containsKey(productId)) {
                String oldInfo = buildPromotionDetailInfo(oldMap.get(productId));
                changes.add(new AuditChange("promotionDetails.removed", oldInfo, null));
            }
        }

        for (UUID productId : newMap.keySet()) {
            if (!oldMap.containsKey(productId)) {
                String newInfo = buildPromotionDetailInfo(newMap.get(productId));
                changes.add(new AuditChange("promotionDetails.added", null, newInfo));
            }
        }

        for (UUID productId : newMap.keySet()) {
            if (oldMap.containsKey(productId)) {
                PromotionDetail oldDetail = oldMap.get(productId);
                PromotionDetail newDetail = newMap.get(productId);

                if (!Objects.equals(oldDetail.getPromotionType(), newDetail.getPromotionType())) {
                    changes.add(new AuditChange(
                            "promotionDetails.type." + productId,
                            oldDetail.getPromotionType() != null ? oldDetail.getPromotionType().name() : null,
                            newDetail.getPromotionType() != null ? newDetail.getPromotionType().name() : null
                    ));
                }

                if (!Objects.equals(oldDetail.getDiscountValue(), newDetail.getDiscountValue())) {
                    changes.add(new AuditChange(
                            "promotionDetails.discount." + productId,
                            oldDetail.getDiscountValue() != null ? oldDetail.getDiscountValue().toString() : null,
                            newDetail.getDiscountValue() != null ? newDetail.getDiscountValue().toString() : null
                    ));
                }
            }
        }
        return changes;
    }

    private String buildPromotionDetailInfo(PromotionDetail detail) {
        if (detail == null) return null;

        // ⭐ Tận dụng PromotionDetailResponse
        PromotionDetailResponse info = PromotionDetailResponse.builder()
                .productId(detail.getProduct() != null ? detail.getProduct().getProductId() : null)
                .productName(detail.getProduct() != null ? detail.getProduct().getName() : null)
                .productCode(detail.getProduct() != null ? detail.getProduct().getProductCode() : null)
                .salePrice(detail.getProduct() != null ? detail.getProduct().getSalePrice() : null)
                .promotionType(detail.getPromotionType())
                .discountValue(detail.getDiscountValue())
                .build();

        // ⭐ Convert sang JSON để FE parse
        try {
            return new ObjectMapper().writeValueAsString(info);
        } catch (Exception e) {
            log.warn("Failed to serialize PromotionDetailResponse: {}", e.getMessage());
            return null;
        }
    }
    private List<AuditChange> buildBatchChanges(ProductBatch oldBatch, ProductBatch newBatch) {
        List<AuditChange> changes = new ArrayList<>();

        // Số lượng tồn kho
        if (!Objects.equals(oldBatch.getStockQuantity(), newBatch.getStockQuantity())) {
            changes.add(new AuditChange("stockQuantity",
                    oldBatch.getStockQuantity() != null ? oldBatch.getStockQuantity().toString() : null,
                    newBatch.getStockQuantity() != null ? newBatch.getStockQuantity().toString() : null
            ));
        }

        // Ngày hết hạn
        if (!Objects.equals(oldBatch.getExpiryDate(), newBatch.getExpiryDate())) {
            changes.add(new AuditChange("expiryDate",
                    oldBatch.getExpiryDate() != null ? oldBatch.getExpiryDate().toString() : null,
                    newBatch.getExpiryDate() != null ? newBatch.getExpiryDate().toString() : null
            ));
        }

        // Trạng thái
        if (!Objects.equals(oldBatch.getStatus(), newBatch.getStatus())) {
            changes.add(new AuditChange("status",
                    oldBatch.getStatus() != null ? oldBatch.getStatus().name() : null,
                    newBatch.getStatus() != null ? newBatch.getStatus().name() : null
            ));
        }

        // Giá nhập
        if (!Objects.equals(oldBatch.getBasePrice(), newBatch.getBasePrice())) {
            changes.add(new AuditChange("basePrice",
                    oldBatch.getBasePrice() != null ? oldBatch.getBasePrice().toString() : null,
                    newBatch.getBasePrice() != null ? newBatch.getBasePrice().toString() : null
            ));
        }

        // Soft delete
        if (!Objects.equals(oldBatch.getDeletedAt(), newBatch.getDeletedAt())) {
            changes.add(new AuditChange("deletedAt",
                    oldBatch.getDeletedAt() != null ? oldBatch.getDeletedAt().toString() : null,
                    newBatch.getDeletedAt() != null ? newBatch.getDeletedAt().toString() : null
            ));
        }

        return changes;
    }

    private AuditLog buildAuditLog(
            AuditEntityType entityType,
            UUID entityId,
            List<AuditChange> changes,
            String reason,
            String note,
            User performedBy) {

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(AuditAction.UPDATE);
        auditLog.setChanges(changes);
        auditLog.setReason(reason);
        auditLog.setNote(note);
        auditLog.setPerformedBy(performedBy);
        auditLog.setPerformedAt(LocalDateTime.now());

        return auditLog;
    }

    private Specification<AuditLog> buildFilterSpecification(AuditLogFilterRequest filterRequest) {
        Specification<AuditLog> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (filterRequest.getEntityType() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("entityType"), filterRequest.getEntityType()));
        }

        if (filterRequest.getEntityId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("entityId"), filterRequest.getEntityId()));
        }

        if (filterRequest.getAction() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("action"), filterRequest.getAction()));
        }

        if (filterRequest.getFromDate() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("performedAt"), filterRequest.getFromDate()));
        }

        if (filterRequest.getToDate() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("performedAt"), filterRequest.getToDate()));
        }

        if (filterRequest.getPerformedBy() != null && !filterRequest.getPerformedBy().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("performedBy").get("email"), filterRequest.getPerformedBy()));
        }

        return spec;
    }
}