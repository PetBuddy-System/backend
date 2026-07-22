package com.petbuddy.petbuddystore.dto.response;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VnPayRefundResponse {
    String responseCode;
    String transactionNo;
    String message;
    boolean success;
}
