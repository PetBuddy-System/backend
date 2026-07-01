package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.PaymentMethod;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.model.Order;

public interface PaymentService {
    void createPayment(Order order, PaymentMethod method);
    void handleWebhook(String payload, String sigHeader);
    PaymentResponse getPaymentByOrderId(Long orderId);
    void markPaymentSucceeded(Order order);
}
