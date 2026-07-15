package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.AuditAction;
import com.petbuddy.petbuddystore.common.enums.AuditEntityType;
import com.petbuddy.petbuddystore.common.enums.PaymentStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.AuditLogFilterRequest;
import com.petbuddy.petbuddystore.dto.response.AuditLogResponse;
import com.petbuddy.petbuddystore.mapper.AuditLogMapper;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.*;
import com.petbuddy.petbuddystore.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final ProductBatchRepository productBatchRepository;

    // ============================================================
    // 1. PRODUCT CREATE
    // ============================================================

    @Override
    @Transactional
    public void logProductCreate(Product product, String reason, String note, User performedBy) {
        List<AuditChange> changes = buildProductCreateChanges(product);

        AuditLog auditLog = AuditLog.builder()
                .entityType(AuditEntityType.PRODUCT)
                .entityId(product.getProductId())
                .entityCode(product.getProductCode())  // ⭐ THÊM entityCode
                .action(AuditAction.CREATE)
                .changes(changes)
                .reason(reason != null ? reason : "CREATE_PRODUCT")
                .note(note)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit log created for Product CREATE: {}", product.getProductId());
    }

    // ============================================================
    // 2. PRODUCT UPDATE
    // ============================================================

    @Override
    @Transactional
    public void logProductUpdate(Product oldProduct, Product newProduct, String reason, String note, User performedBy) {
        List<AuditChange> changes = buildProductChanges(oldProduct, newProduct);

        if (changes.isEmpty()) {
            log.info("No changes detected for Product ID: {}", oldProduct.getProductId());
            return;
        }

        AuditLog auditLog = AuditLog.builder()
                .entityType(AuditEntityType.PRODUCT)
                .entityId(oldProduct.getProductId())
                .entityCode(oldProduct.getProductCode())  // ⭐ THÊM entityCode
                .action(AuditAction.UPDATE)
                .changes(changes)
                .reason(reason != null ? reason : "UPDATE_PRODUCT")
                .note(note)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit log created for Product ID: {}", oldProduct.getProductId());
    }

    // ============================================================
    // 3. PROMOTION CREATE
    // ============================================================

    @Override
    @Transactional
    public void logPromotionCreate(Promotion promotion, String reason, String note, User performedBy) {
        List<AuditChange> changes = buildPromotionCreateChanges(promotion);

        AuditLog auditLog = AuditLog.builder()
                .entityType(AuditEntityType.PROMOTION)
                .entityId(promotion.getPromotionId())
                .entityCode(promotion.getPromotionCode())  // ⭐ THÊM entityCode
                .action(AuditAction.CREATE)
                .changes(changes)
                .reason(reason != null ? reason : "CREATE_PROMOTION")
                .note(note)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit log created for Promotion CREATE: {}", promotion.getPromotionId());
    }

    // ============================================================
    // 4. PROMOTION UPDATE
    // ============================================================

    @Override
    @Transactional
    public void logPromotionUpdate(Promotion oldPromotion, Promotion newPromotion, String reason, String note, User performedBy) {
        List<AuditChange> changes = buildPromotionChanges(oldPromotion, newPromotion);

        if (changes.isEmpty()) {
            log.info("No changes detected for Promotion ID: {}", oldPromotion.getPromotionId());
            return;
        }

        AuditLog auditLog = AuditLog.builder()
                .entityType(AuditEntityType.PROMOTION)
                .entityId(oldPromotion.getPromotionId())
                .entityCode(oldPromotion.getPromotionCode())  // ⭐ THÊM entityCode
                .action(AuditAction.UPDATE)
                .changes(changes)
                .reason(reason != null ? reason : "UPDATE_PROMOTION")
                .note(note)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit log created for Promotion ID: {}", oldPromotion.getPromotionId());
    }

    // ============================================================
    // 5. BATCH CREATE
    // ============================================================

    @Override
    @Transactional
    public void logBatchCreate(ProductBatch batch, String reason, String note, User performedBy) {
        List<AuditChange> changes = buildBatchCreateChanges(batch);

        AuditLog auditLog = AuditLog.builder()
                .entityType(AuditEntityType.BATCH)
                .entityId(batch.getBatchId())
                .entityCode(batch.getBatchCode())  // ⭐ THÊM entityCode
                .action(AuditAction.CREATE)
                .changes(changes)
                .reason(reason != null ? reason : "CREATE_BATCH")
                .note(note)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit log created for Batch CREATE: {}", batch.getBatchId());
    }

    // ============================================================
    // 6. BATCH UPDATE
    // ============================================================

    @Override
    @Transactional
    public void logBatchUpdate(ProductBatch oldBatch, ProductBatch newBatch, String reason, String note, User performedBy) {
        List<AuditChange> changes = buildBatchChanges(oldBatch, newBatch);

        if (changes.isEmpty()) {
            log.info("No changes detected for Batch ID: {}", oldBatch.getBatchId());
            return;
        }

        AuditLog auditLog = AuditLog.builder()
                .entityType(AuditEntityType.BATCH)
                .entityId(oldBatch.getBatchId())
                .entityCode(oldBatch.getBatchCode())  // ⭐ THÊM entityCode
                .action(AuditAction.UPDATE)
                .changes(changes)
                .reason(reason != null ? reason : "UPDATE_BATCH")
                .note(note)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit log created for Batch ID: {}", oldBatch.getBatchId());
    }

    // ============================================================
    // 7. FILTER METHODS
    // ============================================================

    @Override
    public Page<AuditLogResponse> filterAuditLogs(
            AuditEntityType entityType,
            String entityCode,
            String action,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String performedBy,
            Pageable pageable
    ) {
        AuditLogFilterRequest filterRequest = new AuditLogFilterRequest();
        filterRequest.setEntityType(entityType);
        filterRequest.setEntityCode(entityCode);
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

        Specification<AuditLog> spec = buildFilterSpecification(filterRequest);
        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);
        return page.map(auditLogMapper::toSummaryResponse);
    }

    // ============================================================
    // 8. GET AUDIT LOG BY ID
    // ============================================================

    @Override
    public AuditLogResponse getAuditLogById(UUID id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.AUDIT_LOG_NOT_FOUND));
        return auditLogMapper.toDetailResponse(auditLog);
    }

    // ============================================================
    // 9. BUILD PRODUCT CREATE CHANGES
    // ============================================================

    private List<AuditChange> buildProductCreateChanges(Product product) {
        List<AuditChange> changes = new ArrayList<>();
        changes.add(AuditChange.builder().field("productCode").oldValue(null).newValue(product.getProductCode()).build());
        changes.add(AuditChange.builder().field("name").oldValue(null).newValue(product.getName()).build());
        changes.add(AuditChange.builder().field("description").oldValue(null).newValue(product.getDescription()).build());
        changes.add(AuditChange.builder().field("ingredients").oldValue(null).newValue(product.getIngredients()).build());
        changes.add(AuditChange.builder().field("usageInstructions").oldValue(null).newValue(product.getUsageInstructions()).build());
        changes.add(AuditChange.builder().field("salePrice").oldValue(null).newValue(product.getSalePrice() != null ? product.getSalePrice().toString() : null).build());
        changes.add(AuditChange.builder().field("unit").oldValue(null).newValue(product.getUnit() != null ? product.getUnit().name() : null).build());
        changes.add(AuditChange.builder().field("brandName").oldValue(null).newValue(product.getBrandName()).build());
        changes.add(AuditChange.builder().field("status").oldValue(null).newValue(product.getStatus() != null ? product.getStatus().name() : null).build());
        return changes;
    }

    // ============================================================
    // 10. BUILD PRODUCT CHANGES (UPDATE)
    // ============================================================

    private List<AuditChange> buildProductChanges(Product oldProduct, Product newProduct) {
        List<AuditChange> changes = new ArrayList<>();
        addIfChanged(changes, "name", oldProduct.getName(), newProduct.getName());
        addIfChanged(changes, "description", oldProduct.getDescription(), newProduct.getDescription());
        addIfChanged(changes, "ingredients", oldProduct.getIngredients(), newProduct.getIngredients());
        addIfChanged(changes, "usageInstructions", oldProduct.getUsageInstructions(), newProduct.getUsageInstructions());
        addIfChanged(changes, "salePrice", oldProduct.getSalePrice() != null ? oldProduct.getSalePrice().toString() : null, newProduct.getSalePrice() != null ? newProduct.getSalePrice().toString() : null);
        addIfChanged(changes, "unit", oldProduct.getUnit() != null ? oldProduct.getUnit().name() : null, newProduct.getUnit() != null ? newProduct.getUnit().name() : null);
        addIfChanged(changes, "brandName", oldProduct.getBrandName(), newProduct.getBrandName());
        addIfChanged(changes, "status", oldProduct.getStatus() != null ? oldProduct.getStatus().name() : null, newProduct.getStatus() != null ? newProduct.getStatus().name() : null);
        addIfChanged(changes, "deletedAt", oldProduct.getDeletedAt() != null ? oldProduct.getDeletedAt().toString() : null, newProduct.getDeletedAt() != null ? newProduct.getDeletedAt().toString() : null);
        addIfChanged(changes, "productCode", oldProduct.getProductCode(), newProduct.getProductCode());
        return changes;
    }

    // ============================================================
    // 11. BUILD PROMOTION CREATE CHANGES
    // ============================================================

    private List<AuditChange> buildPromotionCreateChanges(Promotion promotion) {
        List<AuditChange> changes = new ArrayList<>();
        changes.add(AuditChange.builder().field("promotionCode").oldValue(null).newValue(promotion.getPromotionCode()).build());
        changes.add(AuditChange.builder().field("name").oldValue(null).newValue(promotion.getName()).build());
        changes.add(AuditChange.builder().field("description").oldValue(null).newValue(promotion.getDescription()).build());
        changes.add(AuditChange.builder().field("startDate").oldValue(null).newValue(promotion.getStartDate() != null ? promotion.getStartDate().toString() : null).build());
        changes.add(AuditChange.builder().field("endDate").oldValue(null).newValue(promotion.getEndDate() != null ? promotion.getEndDate().toString() : null).build());
        changes.add(AuditChange.builder().field("status").oldValue(null).newValue(promotion.getStatus() != null ? promotion.getStatus().name() : null).build());

        if (promotion.getPromotionDetails() != null && !promotion.getPromotionDetails().isEmpty()) {
            for (PromotionDetail detail : promotion.getPromotionDetails()) {
                changes.add(AuditChange.builder()
                        .field("promotionDetail")
                        .oldValue(null)
                        .newValue(formatDetailInfo(detail))
                        .build());
            }
        }
        return changes;
    }

    // ============================================================
    // 12. BUILD PROMOTION CHANGES (UPDATE)
    // ============================================================

    private List<AuditChange> buildPromotionChanges(Promotion oldPromotion, Promotion newPromotion) {
        List<AuditChange> changes = new ArrayList<>();

        addIfChanged(changes, "name", oldPromotion.getName(), newPromotion.getName());
        addIfChanged(changes, "description", oldPromotion.getDescription(), newPromotion.getDescription());
        addIfChanged(changes, "startDate", oldPromotion.getStartDate() != null ? oldPromotion.getStartDate().toString() : null, newPromotion.getStartDate() != null ? newPromotion.getStartDate().toString() : null);
        addIfChanged(changes, "endDate", oldPromotion.getEndDate() != null ? oldPromotion.getEndDate().toString() : null, newPromotion.getEndDate() != null ? newPromotion.getEndDate().toString() : null);
        addIfChanged(changes, "status", oldPromotion.getStatus() != null ? oldPromotion.getStatus().name() : null, newPromotion.getStatus() != null ? newPromotion.getStatus().name() : null);
        addIfChanged(changes, "deletedAt", oldPromotion.getDeletedAt() != null ? oldPromotion.getDeletedAt().toString() : null, newPromotion.getDeletedAt() != null ? newPromotion.getDeletedAt().toString() : null);
        addIfChanged(changes, "promotionCode", oldPromotion.getPromotionCode(), newPromotion.getPromotionCode());
        changes.addAll(buildPromotionDetailChanges(oldPromotion.getPromotionDetails(), newPromotion.getPromotionDetails()));

        return changes;
    }

    // ============================================================
    // 13. BUILD PROMOTION DETAIL CHANGES
    // ============================================================

    private List<AuditChange> buildPromotionDetailChanges(List<PromotionDetail> oldDetails, List<PromotionDetail> newDetails) {
        List<AuditChange> changes = new ArrayList<>();

        Map<UUID, PromotionDetail> oldMap = oldDetails != null ? oldDetails.stream().collect(Collectors.toMap(d -> d.getProduct().getProductId(), d -> d, (a, b) -> a)) : new HashMap<>();
        Map<UUID, PromotionDetail> newMap = newDetails != null ? newDetails.stream().collect(Collectors.toMap(d -> d.getProduct().getProductId(), d -> d, (a, b) -> a)) : new HashMap<>();

        // ⭐ REMOVED
        for (UUID productId : oldMap.keySet()) {
            if (!newMap.containsKey(productId)) {
                PromotionDetail detail = oldMap.get(productId);
                changes.add(AuditChange.builder()
                        .field("promotionDetail")
                        .oldValue(formatDetailInfo(detail))
                        .newValue(null)
                        .build());
            }
        }

        // ⭐ ADDED
        for (UUID productId : newMap.keySet()) {
            if (!oldMap.containsKey(productId)) {
                PromotionDetail detail = newMap.get(productId);
                changes.add(AuditChange.builder()
                        .field("promotionDetail")
                        .oldValue(null)
                        .newValue(formatDetailInfo(detail))
                        .build());
            }
        }

        // ⭐ UPDATED
        for (UUID productId : newMap.keySet()) {
            if (oldMap.containsKey(productId)) {
                PromotionDetail oldD = oldMap.get(productId);
                PromotionDetail newD = newMap.get(productId);

                boolean typeChanged = !Objects.equals(oldD.getPromotionType(), newD.getPromotionType());
                boolean valueChanged = !Objects.equals(oldD.getDiscountValue(), newD.getDiscountValue());

                if (typeChanged || valueChanged) {
                    changes.add(AuditChange.builder()
                            .field("promotionDetail")
                            .oldValue(formatDetailInfo(oldD))
                            .newValue(formatDetailInfo(newD))
                            .build());
                }
            }
        }

        return changes;
    }

    private String formatDetailInfo(PromotionDetail detail) {
        if (detail == null) return null;
        String productCode = detail.getProduct() != null ? detail.getProduct().getProductCode() : "N/A";
        String productName = detail.getProduct() != null ? detail.getProduct().getName() : "N/A";
        String type = detail.getPromotionType() != null ? detail.getPromotionType().name() : "N/A";
        String value = detail.getDiscountValue() != null ? detail.getDiscountValue().toString() : "N/A";
        return String.format("%s|%s|%s|%s", productCode, productName, type, value);
    }

    // ============================================================
    // 14. BUILD BATCH CREATE CHANGES
    // ============================================================

    private List<AuditChange> buildBatchCreateChanges(ProductBatch batch) {
        List<AuditChange> changes = new ArrayList<>();
        changes.add(AuditChange.builder().field("batchCode").oldValue(null).newValue(batch.getBatchCode()).build());
        changes.add(AuditChange.builder().field("stockQuantity").oldValue(null).newValue(batch.getStockQuantity() != null ? batch.getStockQuantity().toString() : null).build());
        changes.add(AuditChange.builder().field("expiryDate").oldValue(null).newValue(batch.getExpiryDate() != null ? batch.getExpiryDate().toString() : null).build());
        changes.add(AuditChange.builder().field("status").oldValue(null).newValue(batch.getStatus() != null ? batch.getStatus().name() : null).build());
        changes.add(AuditChange.builder().field("basePrice").oldValue(null).newValue(batch.getBasePrice() != null ? batch.getBasePrice().toString() : null).build());
        return changes;
    }

    // ============================================================
    // 15. BUILD BATCH CHANGES (UPDATE)
    // ============================================================

    private List<AuditChange> buildBatchChanges(ProductBatch oldBatch, ProductBatch newBatch) {
        List<AuditChange> changes = new ArrayList<>();

        addIfChanged(changes, "stockQuantity",
                oldBatch.getStockQuantity() != null ? oldBatch.getStockQuantity().toString() : null,
                newBatch.getStockQuantity() != null ? newBatch.getStockQuantity().toString() : null);
        addIfChanged(changes, "expiryDate",
                oldBatch.getExpiryDate() != null ? oldBatch.getExpiryDate().toString() : null,
                newBatch.getExpiryDate() != null ? newBatch.getExpiryDate().toString() : null);
        addIfChanged(changes, "status",
                oldBatch.getStatus() != null ? oldBatch.getStatus().name() : null,
                newBatch.getStatus() != null ? newBatch.getStatus().name() : null);
        addIfChanged(changes, "basePrice",
                oldBatch.getBasePrice() != null ? oldBatch.getBasePrice().toString() : null,
                newBatch.getBasePrice() != null ? newBatch.getBasePrice().toString() : null);
        addIfChanged(changes, "deletedAt",
                oldBatch.getDeletedAt() != null ? oldBatch.getDeletedAt().toString() : null,
                newBatch.getDeletedAt() != null ? newBatch.getDeletedAt().toString() : null);
        addIfChanged(changes, "batchCode", oldBatch.getBatchCode(), newBatch.getBatchCode());

        return changes;
    }

    // ============================================================
    // 16. HELPER: ADD IF CHANGED
    // ============================================================

    private void addIfChanged(List<AuditChange> changes, String field, String oldValue, String newValue) {
        if (field.equals("salePrice") || field.equals("basePrice") || field.equals("price")) {
            try {
                BigDecimal oldNum = new BigDecimal(oldValue != null ? oldValue.trim() : "0");
                BigDecimal newNum = new BigDecimal(newValue != null ? newValue.trim() : "0");
                if (oldNum.compareTo(newNum) == 0) {
                    return; // Không thay đổi
                }
            } catch (NumberFormatException ignored) {}
        }

        if (!Objects.equals(oldValue, newValue)) {
            changes.add(AuditChange.builder()
                    .field(field)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .build());
        }
    }

    // ============================================================
// 17. BUILD FILTER SPECIFICATION
// ============================================================

    private Specification<AuditLog> buildFilterSpecification(AuditLogFilterRequest filterRequest) {
        Specification<AuditLog> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (filterRequest.getEntityType() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("entityType"), filterRequest.getEntityType()));
        }

        // ⭐ SỬA: Dùng IN với danh sách UUID
        if (filterRequest.getEntityCode() != null && !filterRequest.getEntityCode().isEmpty()) {
            String keyword = filterRequest.getEntityCode().trim();
            List<UUID> entityIds = findEntityIdsByCodeOrName(keyword, filterRequest.getEntityType());

            if (!entityIds.isEmpty()) {
                spec = spec.and((root, query, cb) ->
                        root.get("entityId").in(entityIds));
            } else {
                spec = spec.and((root, query, cb) -> cb.disjunction());
                log.warn("Entity not found with keyword: {}", keyword);
            }
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
                    cb.like(cb.lower(root.get("performedBy").get("email")),
                            "%" + filterRequest.getPerformedBy().toLowerCase() + "%"));
        }
        return spec;
    }

// ============================================================
// 18. HELPER: TÌM DANH SÁCH ENTITY ID TỪ CODE HOẶC NAME
// ============================================================

    private List<UUID> findEntityIdsByCodeOrName(String keyword, AuditEntityType entityType) {
        List<UUID> ids = new ArrayList<>();

        if (keyword == null || keyword.isEmpty()) {
            return ids;
        }

        String[] terms = keyword.trim().toLowerCase().split("\\s+");

        // ===== 1. Tìm theo CODE (LIKE) =====

        // Tìm trong Product
        if (entityType == null || entityType == AuditEntityType.PRODUCT) {
            // Tìm chính xác
            productRepository.findByProductCode(keyword)
                    .ifPresent(p -> ids.add(p.getProductId()));

            // Tìm LIKE (prefix)
            List<Product> products = productRepository.findByProductCodeContaining(keyword);
            products.forEach(p -> ids.add(p.getProductId()));
        }

        // Tìm trong Promotion
        if (entityType == null || entityType == AuditEntityType.PROMOTION) {
            promotionRepository.findByPromotionCode(keyword)
                    .ifPresent(p -> ids.add(p.getPromotionId()));

            List<Promotion> promotions = promotionRepository.findByPromotionCodeContaining(keyword);
            promotions.forEach(p -> ids.add(p.getPromotionId()));
        }

        // Tìm trong Batch
        if (entityType == null || entityType == AuditEntityType.BATCH) {
            productBatchRepository.findByBatchCode(keyword)
                    .ifPresent(b -> ids.add(b.getBatchId()));

            List<ProductBatch> batches = productBatchRepository.findByBatchCodeContaining(keyword);
            batches.forEach(b -> ids.add(b.getBatchId()));
        }

        // ===== 2. Nếu chưa tìm thấy, tìm theo NAME (LIKE từng từ) =====
        if (ids.isEmpty()) {
            // Tìm trong Product - NAME
            if (entityType == null || entityType == AuditEntityType.PRODUCT) {
                List<Product> products = productRepository.findAll().stream()
                        .filter(p -> {
                            String name = p.getName() != null ? p.getName().toLowerCase() : "";
                            for (String term : terms) {
                                if (!name.contains(term)) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
                products.forEach(p -> ids.add(p.getProductId()));
            }

            // Tìm trong Promotion - NAME
            if (entityType == null || entityType == AuditEntityType.PROMOTION) {
                List<Promotion> promotions = promotionRepository.findAll().stream()
                        .filter(p -> {
                            String name = p.getName() != null ? p.getName().toLowerCase() : "";
                            for (String term : terms) {
                                if (!name.contains(term)) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
                promotions.forEach(p -> ids.add(p.getPromotionId()));
            }
        }

        return ids;
    }

    @Override
    @Transactional
    public void logPaymentPaid(Payment payment, User performedBy) {
        List<AuditChange> changes = new ArrayList<>();
        changes.add(AuditChange.builder().field("orderCode")
                .oldValue(null)
                .newValue(payment.getOrder() != null ? payment.getOrder().getOrderCode() : null)
                .build());
        changes.add(AuditChange.builder().field("amount")
                .oldValue(null)
                .newValue(payment.getAmount() != null ? payment.getAmount().toString() : null)
                .build());
        changes.add(AuditChange.builder().field("paymentMethod")
                .oldValue(null)
                .newValue(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                .build());
        changes.add(AuditChange.builder().field("status")
                .oldValue(null)
                .newValue(PaymentStatus.PAID.name())
                .build());

        AuditLog auditLog = AuditLog.builder()
                .entityType(AuditEntityType.PAYMENT)
                .entityId(longToUuid(payment.getPaymentId()))
                .entityCode(payment.getOrder() != null ? payment.getOrder().getOrderCode()
                        : ("PAY-" + payment.getPaymentId()))
                .action(AuditAction.PAY)
                .changes(changes)
                .reason("PAYMENT_SUCCEEDED")
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional
    public void logPaymentRefund(Payment payment, BigDecimal refundAmount, boolean isFullRefund,
                                 String reason, User performedBy) {
        List<AuditChange> changes = new ArrayList<>();
        changes.add(AuditChange.builder().field("refundAmount")
                .oldValue(null)
                .newValue(refundAmount != null ? refundAmount.toString() : null)
                .build());
        changes.add(AuditChange.builder().field("totalRefundedAmount")
                .oldValue(null)
                .newValue(payment.getRefundedAmount() != null ? payment.getRefundedAmount().toString() : null)
                .build());
        changes.add(AuditChange.builder().field("status")
                .oldValue(null)
                .newValue(payment.getStatus() != null ? payment.getStatus().name() : null)
                .build());
        changes.add(AuditChange.builder().field("stripeRefundId")
                .oldValue(null)
                .newValue(payment.getStripeRefundId())
                .build());

        AuditLog auditLog = AuditLog.builder()
                .entityType(AuditEntityType.PAYMENT)
                .entityId(longToUuid(payment.getPaymentId()))
                .entityCode(payment.getOrder() != null ? payment.getOrder().getOrderCode()
                        : ("PAY-" + payment.getPaymentId()))
                .action(AuditAction.REFUND)
                .changes(changes)
                .reason(reason != null ? reason : (isFullRefund ? "FULL_REFUND" : "PARTIAL_REFUND"))
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional
    public void logVoucherCreate(Voucher voucher, String reason, String note, User performedBy) {
        List<AuditChange> changes = new ArrayList<>();
        changes.add(AuditChange.builder().field("voucherCode").oldValue(null).newValue(voucher.getVoucherCode()).build());
        changes.add(AuditChange.builder().field("voucherName").oldValue(null).newValue(voucher.getVoucherName()).build());
        changes.add(AuditChange.builder().field("discountType").oldValue(null)
                .newValue(voucher.getDiscountType() != null ? voucher.getDiscountType().name() : null).build());
        changes.add(AuditChange.builder().field("discountValue").oldValue(null)
                .newValue(voucher.getDiscountValue() != null ? voucher.getDiscountValue().toString() : null).build());
        changes.add(AuditChange.builder().field("maxDiscount").oldValue(null)
                .newValue(voucher.getMaxDiscount() != null ? voucher.getMaxDiscount().toString() : null).build());
        changes.add(AuditChange.builder().field("minOrderValue").oldValue(null)
                .newValue(voucher.getMinOrderValue() != null ? voucher.getMinOrderValue().toString() : null).build());
        changes.add(AuditChange.builder().field("applyScope").oldValue(null)
                .newValue(voucher.getApplyScope() != null ? voucher.getApplyScope().name() : null).build());
        changes.add(AuditChange.builder().field("usageLimit").oldValue(null)
                .newValue(voucher.getUsageLimit() != null ? voucher.getUsageLimit().toString() : null).build());
        changes.add(AuditChange.builder().field("perUserLimit").oldValue(null)
                .newValue(voucher.getPerUserLimit() != null ? voucher.getPerUserLimit().toString() : null).build());
        changes.add(AuditChange.builder().field("startAt").oldValue(null)
                .newValue(voucher.getStartAt() != null ? voucher.getStartAt().toString() : null).build());
        changes.add(AuditChange.builder().field("expiredAt").oldValue(null)
                .newValue(voucher.getExpiredAt() != null ? voucher.getExpiredAt().toString() : null).build());
        changes.add(AuditChange.builder().field("status").oldValue(null)
                .newValue(voucher.getStatus() != null ? voucher.getStatus().name() : null).build());

        AuditLog auditLog = AuditLog.builder()
                .entityType(AuditEntityType.VOUCHER)
                .entityId(voucher.getVoucherId())
                .entityCode(voucher.getVoucherCode())
                .action(AuditAction.CREATE)
                .changes(changes)
                .reason(reason != null ? reason : "CREATE_VOUCHER")
                .note(note)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional
    public void logVoucherUsage(UserVouchers userVoucher, User performedBy) {
        List<AuditChange> changes = new ArrayList<>();
        changes.add(AuditChange.builder().field("voucherCode")
                .oldValue(null)
                .newValue(userVoucher.getVoucher() != null ? userVoucher.getVoucher().getVoucherCode() : null)
                .build());
        changes.add(AuditChange.builder().field("userEmail")
                .oldValue(null)
                .newValue(userVoucher.getUser() != null ? userVoucher.getUser().getEmail() : null)
                .build());
        changes.add(AuditChange.builder().field("usedAt")
                .oldValue(null)
                .newValue(userVoucher.getUsedAt() != null ? userVoucher.getUsedAt().toString() : null)
                .build());

        AuditLog auditLog = AuditLog.builder()
                .entityType(AuditEntityType.VOUCHER_USAGE)
                .entityId(userVoucher.getUserVoucherId())
                .entityCode(userVoucher.getVoucher() != null ? userVoucher.getVoucher().getVoucherCode() : null)
                .action(AuditAction.USE)
                .changes(changes)
                .reason("VOUCHER_APPLIED")
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    private UUID longToUuid(Long id) {
        return UUID.nameUUIDFromBytes(("PAYMENT:" + id).toString().getBytes());
    }
}