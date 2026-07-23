package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.OrderStatus;
import com.petbuddy.petbuddystore.common.enums.PaymentMethod;
import com.petbuddy.petbuddystore.common.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryStopResponse {
    Long orderId;
    String orderCode;
    String address;
    String recipientName;
    String phoneNumber;
    Integer sequence;
    Double distanceFromPreviousKm;

    OrderStatus status;
    BigDecimal finalAmount;
    PaymentMethod paymentMethod;
    PaymentStatus paymentStatus;
    LocalDateTime estimatedDeliveryAt;
}
