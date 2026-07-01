package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductBatchUpdateRequest {

    @Min(value = 0, message = "PRODUCT_STOCK_INVALID")
    Integer stockQuantity;

    @Min(value = 0, message = "COST_INVALID")
    BigDecimal cost;

    LocalDate expiryDate;

    ProductStatus status;
}