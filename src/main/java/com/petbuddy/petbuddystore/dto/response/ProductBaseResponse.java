package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class ProductBaseResponse {

    private UUID promotionId;
    private String promotionName;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime promotionEndDate;
    private boolean hasActivePromotion;
    private BigDecimal discountAmount;
    private BigDecimal salePrice;
}