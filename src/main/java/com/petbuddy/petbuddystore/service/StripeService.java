package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.model.Payment;
import com.petbuddy.petbuddystore.model.ReturnRequest;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;

import java.math.BigDecimal;

public interface StripeService {
    PaymentIntent createPaymentIntent(Payment payment);
    PaymentIntent createBookingDepositIntent(Payment payment);
    void cancelIntent(String paymentIntentId);
    Refund createRefund(Payment payment ,BigDecimal refundAmount );
    Refund createRefundForReturn(Payment payment, BigDecimal amount, ReturnRequest returnRequest);
    Event constructEvent(String payload, String sigHeader);
    <T extends StripeObject> T extractStripeObject(Event event, Class<T> type, ErrorCode errorCodeOnFailure);
}
