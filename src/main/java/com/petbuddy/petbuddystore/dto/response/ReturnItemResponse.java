package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnItemResponse {
    Long returnItemId;
    Long orderDetailId;
    String productName;
    String productImage;
    Integer quantity;
    BigDecimal refundAmount;
}
