package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.PaymentMethod;
import com.petbuddy.petbuddystore.dto.response.PaymentInitResponse;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.model.Order;

public interface PaymentService {

    PaymentInitResponse createPayment(Order order, PaymentMethod method);
    void handleWebhook(String payload, String sigHeader);
    PaymentResponse getPaymentByOrderId(Long orderId);
}
