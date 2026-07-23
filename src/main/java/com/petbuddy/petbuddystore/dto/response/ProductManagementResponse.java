package com.petbuddy.petbuddystore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductManagementResponse extends ProductBaseResponse {
    String productCode;
    ProductStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;


    Long nearExpiredStock;
    Long nearExpiredBatchCount;
    LocalDate nearestExpiryDate;
    Integer batchCount;

    UUID promotionId;
}