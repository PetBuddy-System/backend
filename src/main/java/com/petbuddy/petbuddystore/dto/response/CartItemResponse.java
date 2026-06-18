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

    BigDecimal price;

    Integer quantity;

    BigDecimal subtotal;
}
