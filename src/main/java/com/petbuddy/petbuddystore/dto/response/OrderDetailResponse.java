package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDetailResponse {
    Long orderDetailId;
    UUID productId;
    String productName;
    String productImage;
    BigDecimal unitPrice;
    BigDecimal salePrice;
    Integer quantity;
    Integer weight;
    BigDecimal totalPrice;
    LocalDateTime createdAt;
}
