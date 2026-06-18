package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductBatchCreationRequest {

    @NotNull(message = "PRODUCT_STOCK_REQUIRED")
    @Min(value = 0, message = "PRODUCT_STOCK_INVALID")
    Integer stockQuantity;

    LocalDate expiryDate;
}