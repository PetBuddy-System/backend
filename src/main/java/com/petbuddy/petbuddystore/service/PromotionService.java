package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import com.petbuddy.petbuddystore.dto.request.PromotionRequest;
import com.petbuddy.petbuddystore.dto.request.PromotionUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.PromotionListResponse;
import com.petbuddy.petbuddystore.dto.response.PromotionResponse;
import com.petbuddy.petbuddystore.model.Product;
import com.petbuddy.petbuddystore.model.PromotionDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface PromotionService {
    PromotionResponse createPromotion(PromotionRequest request);

    Page<PromotionListResponse> getPromotions(String keyword, PromotionStatus status, Pageable pageable, String sortBy);

    PromotionResponse getPromotionById(UUID id);

    PromotionResponse updatePromotion(UUID id, PromotionUpdateRequest request);

    boolean hasActivePromotion(UUID productId);

    BigDecimal calculateSalePrice(Product product, PromotionDetail detail);
}
