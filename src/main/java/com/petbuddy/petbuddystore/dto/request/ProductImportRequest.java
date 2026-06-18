package com.petbuddy.petbuddystore.dto.request;

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
public class ProductImportRequest {

    int rowNumber;

    String name;

    String description;

    BigDecimal price;

    String brandName;

    String categoryName;

    Integer stockQuantity;

    LocalDate expiryDate;
}