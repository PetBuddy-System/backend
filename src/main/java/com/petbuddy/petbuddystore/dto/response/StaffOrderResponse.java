package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffOrderResponse {
    Long orderId;
    String orderCode;
    String recipientName;
    String phoneNumber;
    String address;
    String status;
    BigDecimal totalAmount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

}
