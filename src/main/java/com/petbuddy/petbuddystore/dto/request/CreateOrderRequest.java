package com.petbuddy.petbuddystore.dto.request;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private String userName;
    private String phoneNumber;
    private String address;
    private String note;
    private String voucherCode;
}
