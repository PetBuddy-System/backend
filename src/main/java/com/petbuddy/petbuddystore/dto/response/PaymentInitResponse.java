package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.PaymentMethod;
import com.petbuddy.petbuddystore.common.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentInitResponse {
    Long paymentId;
    Long orderId;
    String orderCode;
    BigDecimal amount;
    PaymentMethod paymentMethod;
    PaymentStatus status;

    String clientSecret;
}
