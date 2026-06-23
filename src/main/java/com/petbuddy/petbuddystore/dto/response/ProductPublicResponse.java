package com.petbuddy.petbuddystore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
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

    List<String> imageUrls;

    Integer totalStock;
}