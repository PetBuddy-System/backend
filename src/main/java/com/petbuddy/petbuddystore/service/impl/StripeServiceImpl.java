package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.model.Booking;
import com.petbuddy.petbuddystore.model.Payment;
import com.petbuddy.petbuddystore.model.ReturnRequest;
import com.petbuddy.petbuddystore.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StripeServiceImpl implements StripeService {

    @NonFinal
    @Value("${webhook.secret-key}")
    protected String webhookSecret;

    @Override
    public PaymentIntent createPaymentIntent(Payment payment) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(payment.getAmount().longValue())
                    .setCurrency("vnd")
                    .putMetadata("order_id", String.valueOf(payment.getOrder().getOrderId()))
                    .putMetadata("order_code", payment.getOrder().getOrderCode())
                    .build();
            return PaymentIntent.create(params);
        } catch (StripeException ex) {
            log.error("Lỗi tạo Stripe PaymentIntent cho order {}: {}",
                    payment.getOrder().getOrderId(), ex.getMessage());
            throw new AppException(ErrorCode.PAYMENT_STRIPE_ERROR);
        }
    }

    @Override
    public PaymentIntent createBookingDepositIntent(Payment payment) {
        try {
            Booking booking = payment.getBooking();
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(payment.getAmount().longValue())
                    .setCurrency("vnd")
                    .putMetadata("booking_id", String.valueOf(booking.getBookingId()))
                    .putMetadata("booking_code", booking.getBookingCode())
                    .build();
            return PaymentIntent.create(params);
        } catch (StripeException ex) {
            log.error("Lỗi tạo Stripe PaymentIntent cho booking {}: {}",
                    payment.getBooking().getBookingId(), ex.getMessage());
            throw new AppException(ErrorCode.PAYMENT_STRIPE_ERROR);
        }
    }

    @Override
    public void cancelIntent(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

            if ("succeeded".equals(intent.getStatus())) {
                throw new AppException(ErrorCode.PAYMENT_ALREADY_PAID);
            }
            if ("canceled".equals(intent.getStatus())) {
                return;
            }
            intent.cancel();
        } catch (StripeException ex) {
            throw new AppException(ErrorCode.PAYMENT_STRIPE_ERROR);
        }
    }

    @Override
    public Refund createRefund(Payment payment) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId())
                    .putMetadata("order_id", String.valueOf(payment.getOrder().getOrderId()))
                    .putMetadata("order_code", payment.getOrder().getOrderCode())
                    .build();
            return Refund.create(params);
        } catch (StripeException ex) {
            log.error("Lỗi khi tạo refund Stripe cho order {}: {}",
                    payment.getOrder().getOrderId(), ex.getMessage());
            throw new AppException(ErrorCode.PAYMENT_STRIPE_ERROR);
        }
    }

    @Override
    public Refund createRefundForReturn(Payment payment, BigDecimal amount, ReturnRequest returnRequest) {
        try {
            long amountInVnd = amount.longValue();
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId())
                    .setAmount(amountInVnd)
                    .putMetadata("order_id", String.valueOf(payment.getOrder().getOrderId()))
                    .putMetadata("order_code", payment.getOrder().getOrderCode())
                    .putMetadata("return_code", returnRequest.getReturnCode())
                    .putMetadata("refund_amount", amount.toString())
                    .build();
            return Refund.create(params);
        } catch (StripeException ex) {
            log.error("Lỗi khi tạo refund Stripe cho return request {}: {}",
                    returnRequest.getReturnCode(), ex.getMessage());
            throw new AppException(ErrorCode.STRIPE_REFUND_FAILED);
        }
    }

    @Override
    public Event constructEvent(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException ex) {
            throw new AppException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        }
    }

    @Override
    public <T extends StripeObject> T extractStripeObject(Event event, Class<T> type, ErrorCode errorCodeOnFailure) {
        var deserializer = event.getDataObjectDeserializer();
        return deserializer.getObject()
                .map(type::cast)
                .orElseGet(() -> {
                    try {
                        return type.cast(deserializer.deserializeUnsafe());
                    } catch (Exception e) {
                        log.error("Không thể deserialize event {} (type={}): {}",
                                event.getId(), event.getType(), e.getMessage());
                        throw new AppException(errorCodeOnFailure);
                    }
                });
    }
}
