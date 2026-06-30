package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {
    Long orderId;
    String orderCode;
    String recipientName;
    String phoneNumber;
    String address;
    String status;
    BigDecimal finalAmount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<OrderDetailResponse> orderDetails;
}
