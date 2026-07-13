package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryStopResponse {
    Long orderId;
    String orderCode;
    String address;
    String recipientName;
    String phoneNumber;
    int sequence;
    double distanceFromPreviousKm;
}
