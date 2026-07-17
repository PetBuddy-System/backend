package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.PaymentMethod;
import com.petbuddy.petbuddystore.dto.request.MomoIpnRequest;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    Payment createPayment(Order order, PaymentMethod method);
    Payment createBookingDepositPayment(Booking booking, PaymentMethod method);
    void handleWebhook(String payload, String sigHeader);
    PaymentResponse getPaymentByOrderId(Long orderId);
    void markPaymentSucceeded(Order order);
    Page<PaymentResponse> getAllPayments(Pageable pageable);
    PaymentResponse changePaymentMethod(Long orderId, String rawMethod);
    void releaseOrderStock(Order order);
    void cancelPaymentForOrder(Order order);
    void refundForReturn(ReturnRequest returnRequest);
    void cancelStripeIntent(String paymentIntentId);
    void handleMomoIpn(MomoIpnRequest ipn);
    PaymentResponse retryMomoPayment(Long orderId);

}
