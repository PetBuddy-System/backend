package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.PaymentMethod;
import com.petbuddy.petbuddystore.common.enums.PaymentStatus;
import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.mapper.PaymentMapper;
import com.petbuddy.petbuddystore.model.Order;
import com.petbuddy.petbuddystore.model.OrderDetail;
import com.petbuddy.petbuddystore.model.Payment;
import com.petbuddy.petbuddystore.model.ProductBatch;
import com.petbuddy.petbuddystore.repository.OrderRepository;
import com.petbuddy.petbuddystore.repository.PaymentRepository;
import com.petbuddy.petbuddystore.repository.ProductBatchRepository;
import com.petbuddy.petbuddystore.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.transaction.annotation.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentServiceImpl implements PaymentService {

    final PaymentRepository paymentRepository;
    final OrderRepository orderRepository;
    final ProductBatchRepository productBatchRepository;
    final PaymentMapper paymentMapper;

    @Value("${webhook.secret-key}")
    String webhookSecret;

    @Override
    public void createPayment(Order order, PaymentMethod method) {
        if (paymentRepository.existsByOrder_OrderId(order.getOrderId())) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(method)
                .amount(order.getFinalAmount())
                .status(PaymentStatus.PENDING)
                .build();

        order.setPayment(payment);
        paymentRepository.save(payment);

        if (method == PaymentMethod.CARD) {
            createStripePayment(payment);
        }

        paymentRepository.save(payment);
    }



    @Transactional
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

    @Transactional
    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    public void markPaymentSucceeded(Order order) {
        Payment payment = order.getPayment();
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }
        payment.setStatus(PaymentStatus.PAID);
        for (OrderDetail detail : order.getOrderDetails()) {
            deductStockByFefo(detail.getProduct().getProductId(), detail.getQuantity());
        }
        paymentRepository.save(payment);
    }

    private void deductStockByFefo(UUID productId, int quantity) {
        List<ProductBatch> batches = productBatchRepository.findActiveBatchesForUpdate(productId, ProductStatus.ACTIVE);
        List<ProductBatch> updatedBatches = new ArrayList<>();
        int remaining = quantity;

        for (ProductBatch batch : batches) {
            if (remaining <= 0) break;
            int picked = Math.min(batch.getStockQuantity(), remaining);
            batch.setStockQuantity(batch.getStockQuantity() - picked);
            updatedBatches.add(batch);
            remaining -= picked;
        }

        if (remaining > 0) throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        productBatchRepository.saveAll(updatedBatches);
    }

    private String extractPaymentIntentId(Event event) {
        var deserializer = event.getDataObjectDeserializer();

        log.info("Event type: {}, deserializer present: {}",
                event.getType(), deserializer.getObject().isPresent());

        if (deserializer.getObject().isPresent()) {
            String id = ((PaymentIntent) deserializer.getObject().get()).getId();
            log.info("Extracted PaymentIntent ID (object): {}", id);
            return id;
        }

        log.warn("Dùng raw JSON fallback cho event: {}", event.getId());
        try {
            String rawJson = deserializer.getRawJson();
            log.info("Raw JSON: {}", rawJson);
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser
                    .parseString(rawJson)
                    .getAsJsonObject();
            String id = jsonObject.get("id").getAsString();
            log.info("Extracted PaymentIntent ID (raw): {}", id);
            return id;
        } catch (Exception e) {
            log.error("Không thể parse PaymentIntent id: {}", e.getMessage());
            throw new AppException(ErrorCode.PAYMENT_INTENT_NOT_FOUND);
        }
    }

    private void createStripePayment(Payment payment) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(payment.getAmount().longValue())
                    .setCurrency("vnd")
                    .putMetadata("order_id",   String.valueOf(payment.getOrder().getOrderId()))
                    .putMetadata("order_code", payment.getOrder().getOrderCode())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            payment.setStripePaymentIntentId(intent.getId());
            payment.setStripeClientSecret(intent.getClientSecret());
            payment.setStatus(PaymentStatus.PROCESSING);
        } catch (StripeException ex) {
            log.error("Stripe error khi tạo PaymentIntent cho order {}: {}",
                    payment.getOrder().getOrderId(), ex.getMessage());
            throw new AppException(ErrorCode.PAYMENT_STRIPE_ERROR);
        }
    }

    private void handlePaymentSucceeded(Event event) {
        String intentId = extractPaymentIntentId(event);
        Payment payment = findByStripeIntentId(intentId);

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Webhook trùng lặp, bỏ qua: PaymentIntent={}", intentId);
            return;
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Order order = payment.getOrder();

        log.info("Thanh toán thành công: PaymentIntent={}, Order={}", intentId, order.getOrderCode());
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

    private Payment findByStripeIntentId(String intentId) {
        return paymentRepository.findByStripePaymentIntentId(intentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_INTENT_NOT_FOUND));
    }
}
