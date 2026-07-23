package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class CartItemResponse {
    UUID cartItemId;
    UUID productId;
    String productName;
    String description;
    BigDecimal price;
    Integer quantity;
    BigDecimal salePrice;
    String imageUrl;
    BigDecimal subtotal;
    Boolean adjusted;
}
