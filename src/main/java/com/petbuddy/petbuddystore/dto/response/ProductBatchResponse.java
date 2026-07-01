package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductBatchResponse {

    UUID batchId;

    String batchCode;

    Integer stockQuantity;

    BigDecimal cost;

    LocalDate expiryDate;

    ProductStatus status;

    LocalDateTime deletedAt;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}