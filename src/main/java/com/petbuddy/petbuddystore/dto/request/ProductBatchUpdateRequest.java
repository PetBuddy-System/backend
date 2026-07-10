package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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

    @Min(value = 1, message = "PRODUCT_STOCK_INVALID")
    Integer stockQuantity;

    @Digits(integer = 19, fraction = 2, message = "BASE_PRICE_FORMAT_INVALID")
    @Min(value = 0, message = "BASE_PRICE_INVALID")
    BigDecimal basePrice;

    @Future(message = "BATCH_EXPIRY_DATE_INVALID")
    LocalDate expiryDate;

    ProductStatus status;

    @Size(max = 500, message = "BATCH_REASON_INVALID")
    String reason;

    @Size(max = 1000, message = "BATCH_NOTE_INVALID")
    String note;
}