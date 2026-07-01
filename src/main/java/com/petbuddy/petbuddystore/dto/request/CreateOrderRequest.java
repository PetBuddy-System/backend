package com.petbuddy.petbuddystore.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {
    String recipientName;
    String phoneNumber;
    String address;
    String note;
    String voucherCode;
    BigDecimal shippingFee;
    String paymentMethod;
}
