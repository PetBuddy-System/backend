package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.DiscountType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionDetailResponse {
    UUID promotionDetailId;
    UUID productId;
    String productName;
    String productCode;
    BigDecimal price;
    DiscountType discountType;
    BigDecimal discountValue;
    BigDecimal salePrice;
    BigDecimal discountAmount;
}
