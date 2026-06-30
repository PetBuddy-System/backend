package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.OrderStatus;
import com.petbuddy.petbuddystore.common.enums.PaymentMethod;
import com.petbuddy.petbuddystore.common.enums.PaymentStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.response.PaymentInitResponse;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.mapper.PaymentMapper;
import com.petbuddy.petbuddystore.model.Order;
import com.petbuddy.petbuddystore.model.Payment;
import com.petbuddy.petbuddystore.repository.OrderRepository;
import com.petbuddy.petbuddystore.repository.PaymentRepository;
import com.petbuddy.petbuddystore.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentServiceImpl implements PaymentService {

    final PaymentRepository paymentRepository;
    final OrderRepository orderRepository;
    final PaymentMapper paymentMapper;

    @Value("${webhook.secret-key}")
    String webhookSecret;

    @Override
    public PaymentInitResponse createPayment(Order order, PaymentMethod method) {

        if (paymentRepository.existsByOrder_OrderId(order.getOrderId())) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(method)
                .amount(order.getFinalAmount())
                .status(PaymentStatus.PENDING)
                .build();

        if (method == PaymentMethod.CARD) {
            return createStripePayment(payment);
        }

        paymentRepository.save(payment);
        return paymentMapper.toPaymentInitResponse(payment);
    }

    private PaymentInitResponse createStripePayment(Payment payment) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(payment.getAmount().longValue())
                    .setCurrency("vnd")
                    .addPaymentMethodType("card")
                    .putMetadata("order_id",   String.valueOf(payment.getOrder().getOrderId()))
                    .putMetadata("order_code", payment.getOrder().getOrderCode())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            payment.setStripePaymentIntentId(intent.getId());
            payment.setStripeClientSecret(intent.getClientSecret());
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            PaymentInitResponse response = paymentMapper.toPaymentInitResponse(payment);
            response.setClientSecret(intent.getClientSecret());
            return response;

        } catch (StripeException ex) {
            log.error("Stripe error khi tạo PaymentIntent cho order {}: {}",
                    payment.getOrder().getOrderId(), ex.getMessage());
            throw new AppException(ErrorCode.PAYMENT_STRIPE_ERROR);
        }
    }

    @Override
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException ex) {
            log.warn("Webhook signature không hợp lệ: {}", ex.getMessage());
            throw new AppException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        }

        log.info("Nhận Stripe webhook event: {}", event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded"       -> handlePaymentSucceeded(event);
            case "payment_intent.payment_failed"  -> handlePaymentFailed(event);
            case "payment_intent.canceled"        -> handlePaymentCanceled(event);
            default -> log.debug("Bỏ qua event không xử lý: {}", event.getType());
        }
    }

    private void handlePaymentSucceeded(Event event) {
        String intentId = extractPaymentIntentId(event);
        Payment payment = findByStripeIntentId(intentId);

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        log.info("Thanh toán thành công: PaymentIntent={}, Order={}",
                intentId, order.getOrderCode());
    }

    private void handlePaymentFailed(Event event) {
        String intentId = extractPaymentIntentId(event);
        Payment payment = findByStripeIntentId(intentId);

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        log.warn("Thanh toán thất bại: PaymentIntent={}, Order={}",
                intentId, payment.getOrder().getOrderCode());
    }

    private void handlePaymentCanceled(Event event) {
        String intentId = extractPaymentIntentId(event);
        Payment payment = findByStripeIntentId(intentId);

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        log.info("PaymentIntent bị huỷ: {}", intentId);
    }


    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        return paymentMapper.toPaymentResponse(payment);
    }


    private String extractPaymentIntentId(Event event) {
        return event.getDataObjectDeserializer()
                .getObject()
                .map(obj -> ((PaymentIntent) obj).getId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_INTENT_NOT_FOUND));
    }

    private Payment findByStripeIntentId(String intentId) {
        return paymentRepository.findByStripePaymentIntentId(intentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_INTENT_NOT_FOUND));
    }
}
