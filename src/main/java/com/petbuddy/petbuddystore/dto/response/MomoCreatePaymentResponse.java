package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MomoCreatePaymentResponse {
    String partnerCode;
    String requestId;
    String orderId;
    Long amount;
    String responseTime;
    String message;
    Integer resultCode;
    String payUrl;
    String deeplink;
    String qrCodeUrl;
    String orderType;
    String requestType;
    String transId;
}
