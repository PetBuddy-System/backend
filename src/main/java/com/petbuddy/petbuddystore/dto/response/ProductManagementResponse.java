package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductManagementResponse {

    UUID productId;
    
    String productCode;

    String name;

    BigDecimal price;

    String brandName;

    ProductStatus status;

    String thumbnail;

    Integer totalStock;

    Integer batchCount;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}