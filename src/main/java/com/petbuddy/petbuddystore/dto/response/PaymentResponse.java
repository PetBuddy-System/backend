package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.PaymentMethod;
import com.petbuddy.petbuddystore.common.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
    Long paymentId;
    Long orderId;
    String orderCode;
    PaymentMethod paymentMethod;
    PaymentStatus status;
    BigDecimal amount;
    LocalDateTime paidAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
