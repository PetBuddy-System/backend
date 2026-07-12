package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.*;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.mapper.PaymentMapper;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.*;
import com.petbuddy.petbuddystore.service.CartService;
import com.petbuddy.petbuddystore.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    final BookingRepository bookingRepository;
    final UserRepository userRepository;
    final ProductBatchRepository productBatchRepository;
    final OrderBatchLocationRepository orderBatchLocationRepository;
    final CartService cartService;
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
            holdOrderStock(order);
            createStripePayment(payment);
        }
        paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public Payment createBookingDepositPayment(Booking booking, PaymentMethod method) {
        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(method)
                .amount(booking.getDepositAmount())
                .status(PaymentStatus.PENDING)
                .build();

        booking.getPayments().add(payment);
        paymentRepository.save(payment);

        if (method == PaymentMethod.CARD) {
            createStripeBookingDepositPayment(payment);
        }

        return paymentRepository.save(payment);
    }

    @Transactional
    @Override
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException ex) {
            throw new AppException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        }

        switch (event.getType()) {
            case "payment_intent.succeeded"       -> handlePaymentSucceeded(event);
            case "payment_intent.payment_failed"  -> handlePaymentFailed(event);
            case "payment_intent.canceled"        -> handlePaymentCanceled(event);
            case "charge.refunded", "refund.updated" -> handleRefundUpdated(event);
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
        if (payment.getPaymentMethod() == PaymentMethod.CASH) {
            List<OrderBatchLocation> locations = new ArrayList<>();
            for (OrderDetail detail : order.getOrderDetails()) {
                List<AllocatedBatch> allocations =
                        deductStockByFefo(detail.getProduct().getProductId(), detail.getQuantity());
                for (AllocatedBatch alloc : allocations) {
                    locations.add(OrderBatchLocation.builder()
                            .orderDetail(detail)
                            .batch(alloc.batch())
                            .quantity(alloc.quantity())
                            .build());
                }
            }
            orderBatchLocationRepository.saveAll(locations);
        }
        paymentRepository.save(payment);
    }

    @Override
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        Page<Payment> payments = paymentRepository.findAll(pageable);
        return payments.map(paymentMapper::toPaymentResponse);
    }

    @Override
    @Transactional
    public PaymentResponse changePaymentMethod(Long orderId, String rawMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Payment payment = order.getPayment();
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PAID);
        }

        PaymentMethod newMethod = resolvePaymentMethod(rawMethod);
        if (newMethod == payment.getPaymentMethod()) {
            return paymentMapper.toPaymentResponse(payment);
        }

        if (payment.getPaymentMethod() == PaymentMethod.CARD) {
            if (payment.getStripePaymentIntentId() != null) {
                cancelStripeIntent(payment.getStripePaymentIntentId());
                payment.setStripePaymentIntentId(null);
                payment.setStripeClientSecret(null);
            }
            releaseOrderStock(order);
        }

        payment.setPaymentMethod(newMethod);
        payment.setStatus(PaymentStatus.PENDING);

        if (newMethod == PaymentMethod.CARD) {
            holdOrderStock(order);
            createStripePayment(payment);
        }
        paymentRepository.save(payment);
        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    public void releaseOrderStock(Order order) {
        List<OrderBatchLocation> locations =
                orderBatchLocationRepository.findByOrderDetail_Order_OrderId(order.getOrderId());

        if (locations.isEmpty()) {
            return;
        }

        List<ProductBatch> updatedBatches = new ArrayList<>();
        for (OrderBatchLocation loc : locations) {
            ProductBatch batch = loc.getBatch();
            batch.setStockQuantity(batch.getStockQuantity() + loc.getQuantity());
            updatedBatches.add(batch);
        }
        productBatchRepository.saveAll(updatedBatches);
        orderBatchLocationRepository.deleteAll(locations);
    }

    @Override
    @Transactional
    public PaymentResponse requestCancelOrder(Long orderId, String cancelReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Payment payment = order.getPayment();
        payment.setCancelReason(cancelReason);
        order.setStatus(OrderStatus.CANCEL_REQUESTED);
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);
        paymentRepository.save(payment);
        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse confirmCancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        cancelPaymentForOrder(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return paymentMapper.toPaymentResponse(order.getPayment());
    }

    @Override
    @Transactional
    public void cancelPaymentForOrder(Order order) {
        Payment payment = order.getPayment();

        if (payment.getPaymentMethod() == PaymentMethod.CARD && payment.getStatus() == PaymentStatus.PAID) {
            createStripeRefund(payment);
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
        } else {
            if (payment.getPaymentMethod() == PaymentMethod.CARD
                    && payment.getStripePaymentIntentId() != null
                    && payment.getStatus() != PaymentStatus.CANCELLED) {
                cancelStripeIntent(payment.getStripePaymentIntentId());
            }
            releaseOrderStock(order);
            payment.setStatus(PaymentStatus.CANCELLED);
        }

        paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public void refundForReturn(ReturnRequest returnRequest) {
        Payment payment = returnRequest.getOrder().getPayment();

        // ✅ LẤY REFUND METHOD TỪ RETURN REQUEST
        RefundMethod refundMethod = returnRequest.getRefundMethod();

        // ✅ KIỂM TRA PHƯƠNG THỨC HOÀN TIỀN
        if (refundMethod == RefundMethod.STRIPE_PAYMENT) {
            // Hoàn qua Stripe (thẻ)
            if (payment.getPaymentMethod() != PaymentMethod.CARD) {
                throw new AppException(ErrorCode.REFUND_NOT_SUPPORTED);
            }

            if (payment.getStatus() != PaymentStatus.PAID) {
                throw new AppException(ErrorCode.PAYMENT_NOT_PAID);
            }

            BigDecimal refundAmount = returnRequest.getRefundAmount();
            createStripeRefundForReturn(payment, refundAmount, returnRequest);

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
            paymentRepository.save(payment);

        } else if (refundMethod == RefundMethod.BANK_TRANSFER) {
            // Hoàn qua chuyển khoản ngân hàng
            // Staff sẽ xử lý thủ công bên ngoài hệ thống
            // Chỉ cần set status là REFUNDED
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("Yêu cầu hoàn tiền qua chuyển khoản cho return request {}",
                    returnRequest.getReturnCode());
        } else {
            throw new AppException(ErrorCode.REFUND_METHOD_NOT_SUPPORTED);
        }
    }

    private void createStripeRefundForReturn(Payment payment, BigDecimal amount, ReturnRequest returnRequest) {
        try {
            long amountInVnd = amount.longValue();

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId())
                    .setAmount(amountInVnd)  // ← 394.000 (không nhân 100)
                    .putMetadata("order_id", String.valueOf(payment.getOrder().getOrderId()))
                    .putMetadata("order_code", payment.getOrder().getOrderCode())
                    .putMetadata("return_code", returnRequest.getReturnCode())
                    .putMetadata("refund_amount", amount.toString())
                    .build();

            Refund refund = Refund.create(params);
            payment.setStripeRefundId(refund.getId());

        } catch (StripeException ex) {
            log.error("Lỗi khi tạo refund Stripe cho return request {}: {}",
                    returnRequest.getReturnCode(), ex.getMessage());
            throw new AppException(ErrorCode.STRIPE_REFUND_FAILED);
        }
    }

    private void createStripeRefund(Payment payment) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId())
                    .putMetadata("order_id", String.valueOf(payment.getOrder().getOrderId()))
                    .putMetadata("order_code", payment.getOrder().getOrderCode())
                    .build();

            Refund refund = Refund.create(params);
            payment.setStripeRefundId(refund.getId());

        } catch (StripeException ex) {
            log.error("Lỗi khi tạo refund Stripe cho order {}: {}",
                    payment.getOrder().getOrderId(), ex.getMessage());
            throw new AppException(ErrorCode.PAYMENT_STRIPE_ERROR);
        }
    }

    private record AllocatedBatch(ProductBatch batch, int quantity) {}

    private List<AllocatedBatch> deductStockByFefo(UUID productId, int quantity) {
        List<ProductBatch> batches = productBatchRepository.findActiveBatchesForUpdate(productId, ProductStatus.ACTIVE);
        List<ProductBatch> updatedBatches = new ArrayList<>();
        List<AllocatedBatch> allocations = new ArrayList<>();
        int remaining = quantity;

        for (ProductBatch batch : batches) {
            if (remaining <= 0) break;
            int picked = Math.min(batch.getStockQuantity(), remaining);
            if (picked <= 0) continue;
            batch.setStockQuantity(batch.getStockQuantity() - picked);
            updatedBatches.add(batch);
            allocations.add(new AllocatedBatch(batch, picked));
            remaining -= picked;
        }

        if (remaining > 0) throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        productBatchRepository.saveAll(updatedBatches);
        return allocations;
    }

    private void holdOrderStock(Order order) {
        List<OrderBatchLocation> locations = new ArrayList<>();
        for (OrderDetail detail : order.getOrderDetails()) {
            List<AllocatedBatch> allocations = deductStockByFefo(detail.getProduct().getProductId(), detail.getQuantity());
            for (AllocatedBatch alloc : allocations) {
                locations.add(OrderBatchLocation.builder()
                        .orderDetail(detail)
                        .batch(alloc.batch())
                        .quantity(alloc.quantity())
                        .build());
            }
        }

        orderBatchLocationRepository.saveAll(locations);
        log.info("Đã giữ hàng tạm thời cho order {}", order.getOrderCode());
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

    private void createStripeBookingDepositPayment(Payment payment) {
        try {
            Booking booking = payment.getBooking();
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(payment.getAmount().longValue())
                    .setCurrency("vnd")
                    .putMetadata("booking_id", String.valueOf(booking.getBookingId()))
                    .putMetadata("booking_code", booking.getBookingCode())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            payment.setStripePaymentIntentId(intent.getId());
            payment.setStripeClientSecret(intent.getClientSecret());
            payment.setStatus(PaymentStatus.PROCESSING);
        } catch (StripeException ex) {
            log.error("Stripe error khi tạo PaymentIntent cho booking {}: {}",
                    payment.getBooking().getBookingId(), ex.getMessage());
            throw new AppException(ErrorCode.PAYMENT_STRIPE_ERROR);
        }
    }

    private void handlePaymentSucceeded(Event event) {
        String intentId = extractPaymentIntentId(event);
        Payment payment = findByStripeIntentId(intentId);
        if (payment.getOrder() != null) {
            User user = payment.getOrder().getUser();
            Order order = payment.getOrder();
            if (payment.getStatus() == PaymentStatus.PAID
                    || order.getStatus() == OrderStatus.EXPIRED
                    || order.getStatus() == OrderStatus.CANCELLED) {
                return;
            }
            markPaymentSucceeded(order);

            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            cartService.clearCart(order.getUser());
            paymentRepository.save(payment);
            user.setPaymentFailStreak(0);
            return;
        }

        if (payment.getBooking() != null) {
            Booking booking = payment.getBooking();
            if (payment.getStatus() == PaymentStatus.PAID
                    || booking.getBookingStatus() == BookingStatus.CANCELLED
                    || booking.getBookingStatus() == BookingStatus.FAILED) {
                return;
            }
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            booking.setBookingStatus(BookingStatus.PENDING_ACCEPTANCE);
            paymentRepository.save(payment);
            bookingRepository.save(booking);
        }
    }

    private void handlePaymentFailed(Event event) {
        String intentId = extractPaymentIntentId(event);
        Payment payment = findByStripeIntentId(intentId);

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        if (payment.getOrder() != null) {
            releaseOrderStock(payment.getOrder());
        }
        if (payment.getBooking() != null) {
            Booking booking = payment.getBooking();
            booking.setBookingStatus(BookingStatus.FAILED);
            bookingRepository.save(booking);
        }
    }

    private void handlePaymentCanceled(Event event) {
        String intentId = extractPaymentIntentId(event);
        Payment payment = findByStripeIntentId(intentId);

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);
        if (payment.getOrder() != null) {
            releaseOrderStock(payment.getOrder());
        }
        if (payment.getBooking() != null) {
            Booking booking = payment.getBooking();
            booking.setBookingStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
        }
    }

    private void handleRefundUpdated(Event event) {
        var deserializer = event.getDataObjectDeserializer();
        Refund refund;

        if (deserializer.getObject().isPresent()) {
            refund = (Refund) deserializer.getObject().get();
        } else {
            try {
                com.google.gson.JsonObject json = com.google.gson.JsonParser
                        .parseString(deserializer.getRawJson())
                        .getAsJsonObject();
                refund = Refund.retrieve(json.get("id").getAsString());
            } catch (Exception e) {
                return;
            }
        }

        Payment payment = paymentRepository.findByStripeRefundId(refund.getId()).orElse(null);
        if (payment == null) {
            return;
        }

        switch (refund.getStatus()) {
            case "succeeded" -> {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundedAt(LocalDateTime.now());
            }
            case "failed" -> {
                payment.setStatus(PaymentStatus.PAID);
            }
            default -> log.info("Refund {} đang ở trạng thái {}", refund.getId(), refund.getStatus());
        }

        paymentRepository.save(payment);
    }

    private Payment findByStripeIntentId(String intentId) {
        return paymentRepository.findByStripePaymentIntentId(intentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_INTENT_NOT_FOUND));
    }

    private PaymentMethod resolvePaymentMethod(String raw) {
        try {
            return PaymentMethod.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_METHOD);
        }
    }

    public void cancelStripeIntent(String paymentIntentId) {
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
}
