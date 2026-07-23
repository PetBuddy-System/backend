package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class ProductBatchCreationRequest {

    @NotNull(message = "PRODUCT_STOCK_REQUIRED")
    @Min(value = 1, message = "PRODUCT_STOCK_INVALID")
    Integer stockQuantity;

    @NotNull(message = "BASE_PRICE_REQUIRED")
    @Digits(integer = 19, fraction = 2, message = "BASE_PRICE_FORMAT_INVALID")
    @Min(value = 0, message = "BASE_PRICE_INVALID")
    BigDecimal basePrice;

    @Future(message = "BATCH_EXPIRY_DATE_INVALID")
    LocalDate expiryDate;
}