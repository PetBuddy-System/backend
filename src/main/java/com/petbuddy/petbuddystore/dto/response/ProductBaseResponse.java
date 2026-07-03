package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.DiscountType;
import com.petbuddy.petbuddystore.common.enums.ProductUnit;
import com.petbuddy.petbuddystore.common.enums.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class ProductBaseResponse {
    UUID productId;
    String name;
    String description;
    String ingredients;
    String usageInstructions;
    BigDecimal price;
    String brandName;
    ProductUnit unit;

    String thumbnailUrl;

    Long categoryId;
    String categoryName;
    Integer totalStock;

    boolean hasActivePromotion;
    String promotionName;
    PromotionType promotionType;
    BigDecimal discountValue;
    BigDecimal discountAmount;
    BigDecimal salePrice;
    LocalDateTime promotionEndDate;
}