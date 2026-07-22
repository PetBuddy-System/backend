package com.petbuddy.petbuddystore.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MomoCreatePaymentRequest {
     String partnerCode;
     String partnerName;
     String storeId;
     String requestId;
     Long amount;
     String orderId;
     String orderInfo;
     String redirectUrl;
     String ipnUrl;
     String lang;
     String requestType;
     String autoCapture;
     String extraData;
     String signature;
}
