package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.*;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.MomoIpnRequest;
import com.petbuddy.petbuddystore.dto.response.MomoCreatePaymentResponse;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.dto.response.VnPayCreatePaymentResponse;
import com.petbuddy.petbuddystore.dto.response.VnPayRefundResponse;
import com.petbuddy.petbuddystore.mapper.PaymentMapper;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.*;
import com.petbuddy.petbuddystore.service.*;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentServiceImpl implements PaymentService {

    PaymentRepository paymentRepository;
    OrderRepository orderRepository;
    BookingRepository bookingRepository;

    ProductBatchRepository productBatchRepository;
    OrderBatchLocationRepository orderBatchLocationRepository;
    CartService cartService;
    AuditService auditService;
    StripeService stripeService;
    MomoService momoService;
    VnPayService vnPayService;
    EmailService emailService;
    BookingAssignmentService bookingAssignmentService;
    PaymentMapper paymentMapper;

    @Override
    public Payment createPayment(Order order, PaymentMethod method) {
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
        } else if (method == PaymentMethod.MOMO) {
            holdOrderStock(order);
            createMomoPayment(payment);
        } else if (method == PaymentMethod.VNPAY) {
            holdOrderStock(order);
            createVnPayPayment(payment);
        }
        return paymentRepository.save(payment);
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
        Event event = stripeService.constructEvent(payload, sigHeader);

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded"       -> handlePaymentSucceeded(event);
                case "payment_intent.payment_failed"  -> handlePaymentFailed(event);
                case "payment_intent.canceled"        -> handlePaymentCanceled(event);
                case "charge.refunded", "refund.updated" -> handleRefundUpdated(event);
                default -> log.debug("Bỏ qua event không xử lý: {}", event.getType());
            }
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi không xác định khi xử lý webhook event {} (type={}): {}",
                    event.getId(), event.getType(), ex.getMessage(), ex);
            throw new AppException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        }
    }

    @Transactional
    @Override
    public void handleMomoIpn(MomoIpnRequest ipn) {
        if (!momoService.verifyIpnSignature(ipn)) {
            log.warn("Chữ ký IPN MoMo không hợp lệ cho orderId={}", ipn.getOrderId());
            throw new AppException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        }

        Long orderId = Long.valueOf(ipn.getOrderId());
        Payment payment = paymentRepository.findByMomoOrderId(ipn.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (ipn.getResultCode() == 0) {
            if (payment.getStatus() == PaymentStatus.PAID) {
                log.info("Payment order={} đã PAID trước đó, bỏ qua IPN trùng lặp", orderId);
                return;
            }
            Order order = payment.getOrder();
            User user = order.getUser();

            markPaymentSucceeded(order);
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setMomoTransId(String.valueOf(ipn.getTransId()));
            cartService.clearCart(user);
            paymentRepository.save(payment);
            user.setPaymentFailStreak(0);
            auditService.logPaymentPaid(payment,"PAYMENT_BY_MOMO" ,user);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            releaseOrderStock(payment.getOrder());
            log.warn("Thanh toán MoMo thất bại cho orderId={}: {}", orderId, ipn.getMessage());
        }
    }

    @Transactional
    @Override
    public void handleVnPayIpn(Map<String, String> params) {
        String txnRef = params.get("vnp_TxnRef");
        String secureHash = params.get("vnp_SecureHash");

        if (!vnPayService.verifySignature(params, secureHash)) {
            log.warn("Chữ ký IPN VNPay không hợp lệ cho txnRef={}", txnRef);
            throw new AppException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        }

        Payment payment = paymentRepository.findByVnpayTxnRef(txnRef)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Payment txnRef={} đã PAID trước đó, bỏ qua IPN trùng lặp", txnRef);
            return;
        }

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String amountStr = params.get("vnp_Amount");
        String transactionNo = params.get("vnp_TransactionNo");

        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

        long expectedAmount = payment.getAmount().longValue() * 100;
        boolean amountMatches = String.valueOf(expectedAmount).equals(amountStr);

        if (success && amountMatches) {
            Order order = payment.getOrder();
            User user = order.getUser();

            markPaymentSucceeded(order);
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setVnpayTransactionNo(transactionNo);
            cartService.clearCart(user);
            paymentRepository.save(payment);
            user.setPaymentFailStreak(0);
            auditService.logPaymentPaid(payment, "PAYMENT_BY_VNPAY", user);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            releaseOrderStock(payment.getOrder());
            log.warn("Thanh toán VNPay thất bại/không khớp cho txnRef={}: responseCode={}",
                    txnRef, responseCode);
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
                stripeService.cancelIntent(payment.getStripePaymentIntentId());
                payment.setStripePaymentIntentId(null);
                payment.setStripeClientSecret(null);
            }
            releaseOrderStock(order);
        } else if (payment.getPaymentMethod() == PaymentMethod.MOMO) {
            payment.setMomoTransId(null);
            releaseOrderStock(order);
        } else if (payment.getPaymentMethod() == PaymentMethod.VNPAY) {
            payment.setVnpayTxnRef(null);
            payment.setVnpayPayUrl(null);
            releaseOrderStock(order);
        }

        payment.setPaymentMethod(newMethod);
        payment.setStatus(PaymentStatus.PENDING);

        if (newMethod == PaymentMethod.CARD) {
            holdOrderStock(order);
            createStripePayment(payment);
        } else if (newMethod == PaymentMethod.MOMO) {
            holdOrderStock(order);
            createMomoPayment(payment);
        } else if (newMethod == PaymentMethod.VNPAY) {
            holdOrderStock(order);
            createVnPayPayment(payment);
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
    public void cancelPaymentForOrder(Order order) {
        BigDecimal fullAmount = order.getPayment().getAmount();
        refundOrderPayment(order, fullAmount, BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public void refundForReturn(ReturnRequest returnRequest) {
        Payment payment = returnRequest.getOrder().getPayment();
        RefundMethod refundMethod = returnRequest.getRefundMethod();
        Order order = returnRequest.getOrder();

        if (payment.getPaymentMethod() == PaymentMethod.CASH
                || payment.getPaymentMethod() == PaymentMethod.MOMO
                || (refundMethod == RefundMethod.BANK_TRANSFER && payment.getPaymentMethod() != PaymentMethod.VNPAY)) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            emailService.sendRefundSuccessEmail(
                    order.getUser().getEmail(), order, returnRequest.getRefundAmount());
            log.info("Hoàn tiền thủ công (không qua Stripe/VNPay) cho return request {}",
                    returnRequest.getReturnCode());
            return;
        }

        if (payment.getPaymentMethod() == PaymentMethod.VNPAY) {
            BigDecimal refundAmount = validateAndGetRefundAmount(payment, returnRequest.getRefundAmount());

            VnPayRefundResponse vnpayResponse = vnPayService.createRefund(payment, refundAmount, "SYSTEM");
            if (!vnpayResponse.isSuccess()) {
                log.error("VNPay refund thất bại cho txnRef={}: {}", payment.getVnpayTxnRef(), vnpayResponse.getMessage());
                throw new AppException(ErrorCode.PAYMENT_STRIPE_ERROR);
            }

            applyRefundToPayment(payment, refundAmount);
            paymentRepository.save(payment);
            emailService.sendRefundSuccessEmail(order.getUser().getEmail(), order, refundAmount);

            log.info("Đã hoàn tiền {} qua VNPay cho return request {}", refundAmount, returnRequest.getReturnCode());
            return;
        }

        if (payment.getPaymentMethod() != PaymentMethod.CARD) {
            throw new AppException(ErrorCode.REFUND_NOT_SUPPORTED);
        }

        BigDecimal refundAmount = validateAndGetRefundAmount(payment, returnRequest.getRefundAmount());

        Refund refund = stripeService.createRefundForReturn(payment, refundAmount, returnRequest);
        payment.setStripeRefundId(refund.getId());

        applyRefundToPayment(payment, refundAmount);
        paymentRepository.save(payment);
        log.info("Đã hoàn tiền {} cho return request {}", refundAmount, returnRequest.getReturnCode());
    }

    private BigDecimal validateAndGetRefundAmount(Payment payment, BigDecimal requestedRefundAmount) {
        BigDecimal refundedAmount = payment.getRefundedAmount() != null ? payment.getRefundedAmount() : BigDecimal.ZERO;
        BigDecimal remainingAmount = payment.getAmount().subtract(refundedAmount);

        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.NO_REMAINING_AMOUNT_TO_REFUND);
        }
        if (requestedRefundAmount.compareTo(remainingAmount) > 0) {
            throw new AppException(ErrorCode.REFUND_AMOUNT_EXCEEDS_REMAINING);
        }
        return requestedRefundAmount;
    }


    private void applyRefundToPayment(Payment payment, BigDecimal refundAmount) {
        BigDecimal refundedAmount = payment.getRefundedAmount() != null ? payment.getRefundedAmount() : BigDecimal.ZERO;
        payment.setRefundedAmount(refundedAmount.add(refundAmount));
        payment.setRefundedAt(LocalDateTime.now());
        payment.setStatus(payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED);
    }

    @Override
    @Transactional
    public PaymentResponse retryMomoPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        Payment payment = order.getPayment();

        if (payment.getPaymentMethod() != PaymentMethod.MOMO) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_METHOD);
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PAID);
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        createMomoPayment(payment);
        paymentRepository.save(payment);

        return paymentMapper.toPaymentResponse(payment);
    }

    private void createStripePayment(Payment payment) {
        PaymentIntent intent = stripeService.createPaymentIntent(payment);
        payment.setStripePaymentIntentId(intent.getId());
        payment.setStripeClientSecret(intent.getClientSecret());
        payment.setStatus(PaymentStatus.PROCESSING);
    }

    private void createStripeBookingDepositPayment(Payment payment) {
        PaymentIntent intent = stripeService.createBookingDepositIntent(payment);
        payment.setStripePaymentIntentId(intent.getId());
        payment.setStripeClientSecret(intent.getClientSecret());
        payment.setStatus(PaymentStatus.PROCESSING);
    }

    private void createMomoPayment(Payment payment) {
        Order order = payment.getOrder();
        String orderId = order.getOrderCode() + "-" + System.currentTimeMillis();
        String orderInfo = "Thanh toan don hang " + order.getOrderCode();
        Long amount = payment.getAmount().longValue();

        MomoCreatePaymentResponse response = momoService.createQrPayment(amount, orderId, orderInfo);

        payment.setMomoOrderId(orderId);
        payment.setMomoRequestId(response.getRequestId());
        payment.setMomoPayUrl(response.getPayUrl());
        payment.setStatus(PaymentStatus.PROCESSING);
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
    }

    private <T extends StripeObject> T extractStripeObject(Event event, Class<T> type, ErrorCode errorCodeOnFailure) {
        var deserializer = event.getDataObjectDeserializer();

        return deserializer.getObject()
                .map(type::cast)
                .orElseGet(() -> {
                    try {
                        return type.cast(deserializer.deserializeUnsafe());
                    } catch (Exception e) {
                        log.error("Không thể deserialize event {} (type={}), có thể do lệch phiên bản API Stripe: {}",
                                event.getId(), event.getType(), e.getMessage());
                        throw new AppException(errorCodeOnFailure);
                    }
                });
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

            auditService.logPaymentPaid(payment, "PAYMENT_BY_CARD",user);
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
            bookingAssignmentService.assignAfterPaymentSucceeded(booking);
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

        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.PAID) {
            return;
        }
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
        Refund refund = stripeService.extractStripeObject(event, Refund.class, ErrorCode.REFUND_WEBHOOK_PARSE_FAILED);

        Payment payment = paymentRepository.findByStripeRefundId(refund.getId())
                .orElseThrow(() -> new AppException(ErrorCode.REFUND_PAYMENT_NOT_FOUND));
        User user = payment.getOrder() != null ? payment.getOrder().getUser() : null;

        switch (refund.getStatus()) {
            case "succeeded" -> {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundedAt(LocalDateTime.now());
                auditService.logPaymentRefund(payment, payment.getAmount(), payment.getOrder().getCancelReason(), user);
                if (payment.getOrder() != null && user != null) {
                    BigDecimal refundedNow = BigDecimal.valueOf(refund.getAmount());
                    emailService.sendRefundSuccessEmail(user.getEmail(), payment.getOrder(), refundedNow);
                }
            }
            case "failed" -> payment.setStatus(PaymentStatus.PAID);
            case "pending", "requires_action" -> log.info(
                    "Refund {} đang ở trạng thái '{}', chưa cập nhật status payment liên quan",
                    refund.getId(), refund.getStatus());
            default -> log.warn(
                    "Nhận trạng thái refund mới/không xác định '{}' cho refund {}",
                    refund.getStatus(), refund.getId());
        }
        paymentRepository.save(payment);
    }

    private String extractPaymentIntentId(Event event) {
        return stripeService.extractStripeObject(event, PaymentIntent.class, ErrorCode.PAYMENT_INTENT_NOT_FOUND).getId();
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

    @Override
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

    @Override
    @Transactional
    public boolean confirmMomoStatus(Order order) {
        Payment payment = order.getPayment();

        if (payment.getStatus() == PaymentStatus.PAID) {
            return true;
        }
        if (payment.getMomoOrderId() == null || payment.getMomoRequestId() == null) {
            return false;
        }

        MomoCreatePaymentResponse response = momoService.queryTransactionStatus(
                payment.getMomoOrderId(), payment.getMomoRequestId());

        if (response != null && response.getResultCode() != null && response.getResultCode() == 0) {
            User user = order.getUser();
            markPaymentSucceeded(order);
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setMomoTransId(String.valueOf(response.getTransId()));
            cartService.clearCart(user);
            paymentRepository.save(payment);
            user.setPaymentFailStreak(0);
            auditService.logPaymentPaid(payment,"PAYMENT_SUCCESS" ,user);
            log.warn("Order {} đã được thanh toán MoMo thành công nhưng suýt bị expire — đã đồng bộ lại trạng thái", order.getOrderCode());
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void cancelPaymentForOrderWithPenalty(Order order, BigDecimal penaltyAmount) {
        BigDecimal fullAmount = order.getPayment().getAmount();
        BigDecimal refundAmount = fullAmount.subtract(penaltyAmount);
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = BigDecimal.ZERO;
        }
        refundOrderPayment(order, refundAmount, penaltyAmount);
    }
    private void refundOrderPayment(Order order, BigDecimal refundAmount, BigDecimal penaltyAmount) {
        Payment payment = order.getPayment();
        boolean wasPaid = payment.getStatus() == PaymentStatus.PAID;

        if (!wasPaid) {
            if (payment.getPaymentMethod() == PaymentMethod.CASH) {
                if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PICKING) {
                    releaseOrderStock(order);
                }
            } else {
                if (payment.getStripePaymentIntentId() != null && payment.getStatus() != PaymentStatus.CANCELLED) {
                    stripeService.cancelIntent(payment.getStripePaymentIntentId());
                }
                releaseOrderStock(order);
            }
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
            return;
        }

        switch (payment.getPaymentMethod()) {
            case CARD -> {
                if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                    Refund refund = stripeService.createRefund(payment, refundAmount);
                    payment.setStripeRefundId(refund.getId());
                }
            }
            case VNPAY -> {
                if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                    VnPayRefundResponse vnpayResponse = vnPayService.createRefund(payment, refundAmount, "SYSTEM");
                    if (!vnpayResponse.isSuccess()) {
                        log.error("VNPay refund thất bại cho order {}: {}", order.getOrderId(), vnpayResponse.getMessage());
                        throw new AppException(ErrorCode.PAYMENT_STRIPE_ERROR);
                    }
                }
            }

            case MOMO -> log.warn("MoMo refund {} cho order {} cần xử lý thủ công (chưa tích hợp API refund MoMo)",
                    refundAmount, order.getOrderId());
            case CASH -> {
                log.warn("Đơn CASH {} đã thu tiền, hoàn {} cần xử lý thủ công ngoài hệ thống",
                        order.getOrderId(), refundAmount);
            }
            default -> { }
        }

        payment.setRefundedAmount(refundAmount);
        if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
            payment.setCompensationFee(penaltyAmount);
        }
        payment.setStatus(refundAmount.compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        if ((payment.getPaymentMethod() == PaymentMethod.VNPAY || payment.getPaymentMethod() == PaymentMethod.MOMO)
                && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            emailService.sendRefundSuccessEmail(order.getUser().getEmail(), order, refundAmount);
        }
    }
    private void createVnPayPayment(Payment payment) {
        Order order = payment.getOrder();
        String txnRef = order.getOrderCode() + "-" + System.currentTimeMillis();
        String orderInfo = "Thanh toan don hang " + order.getOrderCode();
        Long amount = payment.getAmount().longValue();

        VnPayCreatePaymentResponse response =
                vnPayService.createPaymentUrl(amount, txnRef, orderInfo, getClientIp());

        payment.setVnpayTxnRef(txnRef);
        payment.setVnpayPayUrl(response.getPayUrl());
        payment.setVnpayCreateDate(response.getCreateDate());
        payment.setStatus(PaymentStatus.PROCESSING);
    }

    private String getClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "127.0.0.1";
        }
        HttpServletRequest request = attrs.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    public boolean verifyVnPayReturn(Map<String, String> allParams) {
        String receivedHash = allParams.get("vnp_SecureHash");
        return vnPayService.verifySignature(allParams, receivedHash);
    }
    @Override
    @Transactional
    public PaymentResponse retryVnPayPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        Payment payment = order.getPayment();

        if (payment.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_METHOD);
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PAID);
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        createVnPayPayment(payment);
        paymentRepository.save(payment);
        return paymentMapper.toPaymentResponse(payment);
    }
}
