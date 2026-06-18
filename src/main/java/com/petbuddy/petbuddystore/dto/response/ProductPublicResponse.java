package com.petbuddy.petbuddystore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPublicResponse {

    UUID productId;

    String name;

    BigDecimal price;

    String brandName;

    String thumbnail;

    Integer totalStock;
}