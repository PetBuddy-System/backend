package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.DiscountType;
import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.PromotionRequest;
import com.petbuddy.petbuddystore.dto.request.PromotionDetailRequest;
import com.petbuddy.petbuddystore.dto.request.PromotionUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.PromotionListResponse;
import com.petbuddy.petbuddystore.dto.response.PromotionResponse;
import com.petbuddy.petbuddystore.dto.response.PromotionDetailResponse;
import com.petbuddy.petbuddystore.mapper.PromotionMapper;
import com.petbuddy.petbuddystore.model.Product;
import com.petbuddy.petbuddystore.model.Promotion;
import com.petbuddy.petbuddystore.model.PromotionDetail;
import com.petbuddy.petbuddystore.repository.ProductRepository;
import com.petbuddy.petbuddystore.repository.PromotionDetailRepository;
import com.petbuddy.petbuddystore.repository.PromotionRepository;
import com.petbuddy.petbuddystore.service.PromotionService;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromotionServiceImpl implements PromotionService {

    PromotionRepository promotionRepository;
    ProductRepository productRepository;
    PromotionMapper promotionMapper;
    PromotionDetailRepository promotionDetailRepository;

    @Override
    public PromotionResponse createPromotion(PromotionRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate()) || request.getStartDate().isEqual(request.getEndDate())) {
            throw new AppException(ErrorCode.PROMOTION_INVALID_DATE);
        }

        Promotion promotion = promotionMapper.toPromotion(request);
        if (promotion.getStatus() == null) {
            promotion.setStatus(PromotionStatus.DRAFT);
        }

        if (request.getPromotionDetails() != null) {
            for (PromotionDetailRequest detailReq : request.getPromotionDetails()) {
                Product product = productRepository.findById(detailReq.getProductId())
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                if (product.getStatus() == ProductStatus.DELETED) {
                    throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
                }

                validateDiscount(detailReq.getDiscountType(), detailReq.getDiscountValue(), product.getPrice());

                PromotionDetail detail = PromotionDetail.builder()
                        .promotion(promotion)
                        .product(product)
                        .discountType(detailReq.getDiscountType())
                        .discountValue(detailReq.getDiscountValue())
                        .build();

                promotion.getPromotionDetails().add(detail);
            }
        }

        promotion.setCreatedAt(LocalDateTime.now());
        promotion.setUpdatedAt(LocalDateTime.now());

        Promotion saved = promotionRepository.save(promotion);
        return convertToPromotionResponseWithCalculations(saved);
    }

    @Override
    public Page<PromotionListResponse> getPromotions(String keyword, PromotionStatus status, Pageable pageable, String sortBy) {
        Pageable resolvedPageable = buildPageable(pageable, sortBy);
        Specification<Promotion> spec = buildPromotionSpec(keyword, status);
        return promotionRepository.findAll(spec, resolvedPageable)
                .map(promotionMapper::toListPromotionResponse);
    }

    @Override
    public PromotionResponse getPromotionById(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        if (promotion.getStatus() == PromotionStatus.DELETED || promotion.getDeletedAt() != null) {
            throw new AppException(ErrorCode.PROMOTION_NOT_FOUND);
        }

        return convertToPromotionResponseWithCalculations(promotion);
    }

    @Override
    public PromotionResponse updatePromotion(UUID id, PromotionUpdateRequest request) {
        Promotion promotion = promotionRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        if (promotion.getStatus() == PromotionStatus.DELETED || promotion.getDeletedAt() != null) {
            throw new AppException(ErrorCode.PROMOTION_NOT_FOUND);
        }

        LocalDateTime newStart = request.getStartDate() != null ? request.getStartDate() : promotion.getStartDate();
        LocalDateTime newEnd = request.getEndDate() != null ? request.getEndDate() : promotion.getEndDate();
        if (newStart != null && newEnd != null && (newStart.isAfter(newEnd) || newStart.isEqual(newEnd))) {
            throw new AppException(ErrorCode.PROMOTION_INVALID_DATE);
        }

        promotionMapper.updatePromotionFromRequest(request, promotion);

        if (request.getStatus() != null) {
            if (request.getStatus() == PromotionStatus.DELETED) {
                promotion.setDeletedAt(LocalDateTime.now());
            } else {
                promotion.setDeletedAt(null);
            }
            promotion.setStatus(request.getStatus());
        }

        if (request.getPromotionDetails() != null) {
            promotionDetailRepository.deleteAll(promotion.getPromotionDetails());
            promotion.getPromotionDetails().clear();
            for (PromotionDetailRequest detailReq : request.getPromotionDetails()) {
                Product product = productRepository.findById(detailReq.getProductId())
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                if (product.getStatus() == ProductStatus.DELETED) {
                    throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
                }

                validateDiscount(detailReq.getDiscountType(), detailReq.getDiscountValue(), product.getPrice());

                PromotionDetail detail = PromotionDetail.builder()
                        .promotion(promotion)
                        .product(product)
                        .discountType(detailReq.getDiscountType())
                        .discountValue(detailReq.getDiscountValue())
                        .build();

                promotion.getPromotionDetails().add(detail);
            }
        }

        promotion.setUpdatedAt(LocalDateTime.now());
        Promotion saved = promotionRepository.save(promotion);

        return convertToPromotionResponseWithCalculations(saved);
    }

    @Override
    public boolean hasActivePromotion(UUID productId) {
        return promotionDetailRepository.existsByProduct_ProductIdAndPromotion_Status(productId, PromotionStatus.ACTIVE);
    }

    @Override
    public BigDecimal calculateSalePrice(Product product, PromotionDetail detail) {
        BigDecimal price = product.getPrice();
        BigDecimal discountAmount = calculateDiscountAmount(price, detail.getDiscountType(), detail.getDiscountValue());
        return price.subtract(discountAmount).max(BigDecimal.ZERO);
    }

    private BigDecimal calculateDiscountAmount(BigDecimal price, DiscountType type, BigDecimal value) {
        if (price == null || value == null) {
            return BigDecimal.ZERO;
        }

        if (type == DiscountType.PERCENTAGE) {
            return price.multiply(value)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else if (type == DiscountType.FIXED) {
            return value.min(price);
        }
        return BigDecimal.ZERO;
    }

    private PromotionResponse convertToPromotionResponseWithCalculations(Promotion promotion) {
        PromotionResponse response = promotionMapper.toPromotionResponse(promotion);
        if (response.getPromotionDetails() != null && !response.getPromotionDetails().isEmpty()) {
            for (PromotionDetailResponse detailResponse : response.getPromotionDetails()) {
                PromotionDetail detail = promotion.getPromotionDetails().stream()
                        .filter(d -> d.getPromotionDetailId().equals(detailResponse.getPromotionDetailId()))
                        .findFirst()
                        .orElse(null);
                if (detail != null) {
                    BigDecimal discountAmount = calculateDiscountAmount(
                            detailResponse.getPrice(),
                            detail.getDiscountType(),
                            detail.getDiscountValue()
                    );
                    BigDecimal salePrice = detailResponse.getPrice().subtract(discountAmount);
                    if (salePrice.compareTo(BigDecimal.ZERO) < 0) {
                        salePrice = BigDecimal.ZERO;
                    }
                    detailResponse.setDiscountAmount(discountAmount);
                    detailResponse.setSalePrice(salePrice);
                }
            }
        }
        return response;
    }

    private void validateDiscount(DiscountType type, BigDecimal value, BigDecimal price) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.PROMOTION_DISCOUNT_INVALID);
        }
        if (type == DiscountType.PERCENTAGE) {
            if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new AppException(ErrorCode.PROMOTION_DISCOUNT_INVALID);
            }
        } else if (type == DiscountType.FIXED) {
            if (value.compareTo(price) > 0) {
                throw new AppException(ErrorCode.PROMOTION_DISCOUNT_INVALID);
            }
        }
    }

    private Specification<Promotion> buildPromotionSpec(String keyword, PromotionStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String searchKeyword = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), searchKeyword),
                        cb.like(cb.lower(root.get("description")), searchKeyword)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), PromotionStatus.DELETED));
            }
            predicates.add(cb.isNull(root.get("deletedAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable buildPageable(Pageable pageable, String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt_desc";
        }
        String[] parts = sortBy.split("_");
        String field = parts[0];
        if (field.equals("date")) {
            field = "startDate";
        }
        Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Set<String> allowedFields = Set.of("name", "startDate", "endDate", "status", "createdAt");
        if (!allowedFields.contains(field)) {
            throw new AppException(ErrorCode.INVALID_SORT_OPTION);
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(direction, field));
    }
}