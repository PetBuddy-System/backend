package com.petbuddy.petbuddystore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.petbuddy.petbuddystore.common.enums.DiscountType;
import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductManagementResponse extends ProductBaseResponse {
    // Product info
    UUID productId;
    String productCode;
    String name;
    String description;
    String ingredients;
    String usageInstructions;
    BigDecimal price;
    String brandName;
    ProductStatus status;
    List<String> imageUrls;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Long categoryId;
    String categoryName;

    // Stock info
    Integer totalStock;
    Long nearExpiredStock;
    Long nearExpiredBatchCount;
    LocalDate nearestExpiryDate;
    Integer batchCount;
}