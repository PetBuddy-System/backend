package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.DiscountType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionDetailRequest {

    @NotNull(message = "PROMOTION_PRODUCT_REQUIRED")
    UUID productId;

    @NotNull(message = "PROMOTION_DISCOUNT_TYPE_REQUIRED")
    DiscountType discountType;

    @NotNull(message = "PROMOTION_DISCOUNT_VALUE_REQUIRED")
    BigDecimal discountValue;
}
