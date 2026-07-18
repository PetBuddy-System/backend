package com.petbuddy.petbuddystore.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MomoQueryStatusRequest {
    String partnerCode;
    String requestId;
    String orderId;
    String lang;
    String signature;
}
