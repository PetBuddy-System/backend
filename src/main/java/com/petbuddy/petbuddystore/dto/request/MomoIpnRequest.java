package com.petbuddy.petbuddystore.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MomoIpnRequest {
    String partnerCode;
    String orderId;
    String requestId;
    Long amount;
    String orderInfo;
    String orderType;
    String transId;
    Integer resultCode;
    String message;
    String payType;
    String responseTime;
    String extraData;
    String signature;
}