package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    UUID productId;

    String productCode;

    String name;

    String description;

    BigDecimal price;

    String brandName;

    ProductStatus status;

    Long categoryId;

    String categoryName;

    List<String> imageUrls;

    Integer totalStock;

    Integer batchCount;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}