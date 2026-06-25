package com.petbuddy.petbuddystore.dto.cart;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.web.servlet.FilterRegistration;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class CartItemData {
    UUID cartItemId;

    UUID productId;

    String productName;

    BigDecimal price;

    Integer quantity;

    String imageUrl;

    BigDecimal subtotal;
}
