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
    String note;
    String status;
    BigDecimal finalAmount;
    BigDecimal shippingFee;
    String clientSecret;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<OrderDetailResponse> orderDetails;
    PaymentResponse payment;
    VoucherResponse voucher;
}
