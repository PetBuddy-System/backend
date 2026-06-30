package com.petbuddy.petbuddystore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.petbuddy.petbuddystore.common.enums.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductPublicResponse extends ProductBaseResponse{
    // Product info
    UUID productId;
    String productCode;
    String name;
    String description;
    String ingredients;
    String usageInstructions;
    BigDecimal price;
    String brandName;
    List<String> imageUrls;
    Integer totalStock;
    Long categoryId;
    String categoryName;
}