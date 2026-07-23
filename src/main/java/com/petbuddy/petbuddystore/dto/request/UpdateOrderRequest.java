package com.petbuddy.petbuddystore.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateOrderRequest {
    String recipientName;
    String phoneNumber;
    String address;
    String note;
    String voucherCode;
    Double latitude;
    Double longitude;
}
