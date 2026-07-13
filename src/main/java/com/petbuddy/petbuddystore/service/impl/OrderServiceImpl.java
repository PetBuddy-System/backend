package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.*;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.CreateOrderRequest;
import com.petbuddy.petbuddystore.dto.request.UpdateOrderRequest;
import com.petbuddy.petbuddystore.dto.response.*;
import com.petbuddy.petbuddystore.mapper.OrderMapper;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.*;
import com.petbuddy.petbuddystore.repository.PaymentRepository;
import com.petbuddy.petbuddystore.service.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderServiceImpl implements OrderService {

    OrderRepository orderRepository;
    ProductBatchRepository productBatchRepository;
    UserRepository userRepository;
    OrderDetailRepository orderDetailRepository;
    StaffScheduleRepository staffScheduleRepository;
    ProductService productService;
    CartService cartService;
    PaymentService paymentService;
    VoucherService voucherService;
    EmailService emailService;
    PaymentServiceImpl paymentServiceImpl;
    ShippingRuleService shippingRuleService;
    FileService fileService;
    OrderMapper orderMapper;
    PaymentRepository paymentRepository;

    static int MAX_CONSECUTIVE_FAILS = 4;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        checkLogin();
        User user = getCurrentUser();
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_METHOD);
        }

        List<CartItemResponse> cartItems = cartService.getCart().getCartItems();
        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        ShippingFeeResponse shippingFeeResponse = shippingRuleService.calculateFee(request.getLatitude(), request.getLongitude());
        BigDecimal shippingFee = shippingFeeResponse.getShippingFee();

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .user(user)
                .recipientName(request.getRecipientName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .longitude(request.getLongitude())
                .latitude(request.getLatitude())
                .note(request.getNote())
                .shippingFee(shippingFee)
                .status(OrderStatus.PENDING)
                .paymentExpiredAt(LocalDateTime.now().plusMinutes(1))
                .createdAt(LocalDateTime.now())
                .build();

        List<OrderDetail> orderDetails = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItemResponse item : cartItems) {
            BigDecimal unitPrice = item.getPrice();
            BigDecimal salePrice = item.getSalePrice();
            BigDecimal effectivePrice = salePrice != null ? salePrice : unitPrice;
            Product product = productService.getProductEntityById(item.getProductId());
            int physicalStock = productBatchRepository.findAvailableStockByProductId(product.getProductId());
            int held = orderDetailRepository.sumHeldQuantityByProductId(product.getProductId(), LocalDateTime.now());
            int virtualAvailable = physicalStock - held;

            if (virtualAvailable < item.getQuantity()) {
                throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
            }

            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productImage(getFirstImage(product))
                    .unitPrice(unitPrice)
                    .salePrice(salePrice)
                    .quantity(item.getQuantity())
                    .totalPrice(effectivePrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build();

            total = total.add(detail.getTotalPrice());
            orderDetails.add(detail);
        }

        BigDecimal discountAmount = voucherService.applyVoucherToOrder(order, request.getVoucherCode(), user, total);
        BigDecimal finalAmount = total.subtract(discountAmount);

        order.setOrderDetails(orderDetails);
        order.setTotalAmount(total);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount.add(shippingFee));
        orderRepository.save(order);

        paymentService.createPayment(order, method);
        cartService.clearCart();
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public OrderResponse updateOrder(Long orderId, UpdateOrderRequest request) {
        Order order = getOrderOrThrow(orderId);
        User user = getCurrentUser();

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_CANNOT_BE_UPDATED);
        }

        if (StringUtils.hasText(request.getRecipientName())) {
            order.setRecipientName(request.getRecipientName());
        }
        if (StringUtils.hasText(request.getPhoneNumber())) {
            order.setPhoneNumber(request.getPhoneNumber());
        }
        if (StringUtils.hasText(request.getAddress())) {
            order.setAddress(request.getAddress());
        }

        if (request.getNote() != null) {
            order.setNote(request.getNote());
        }

        if (request.getVoucherCode() != null) {
            voucherService.releaseVoucherFromOrder(order);
            BigDecimal discountAmount = voucherService.applyVoucherToOrder(
                    order, request.getVoucherCode(), user, order.getTotalAmount());
            order.setDiscountAmount(discountAmount);
            order.setFinalAmount(order.getTotalAmount().subtract(discountAmount).add(order.getShippingFee()));
        }
        else {
            order.setDiscountAmount(BigDecimal.ZERO);
        }

        Order updated = orderRepository.save(order);
        return orderMapper.toOrderResponse(updated);
    }

    @Override
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, MultipartFile proofImage) {
        checkLogin();
        User currentUser = getCurrentUser();
        Order order = findOrder(orderId);
        OrderStatus currentStatus = order.getStatus();

        switch (currentStatus) {
            case PENDING -> {
                if (newStatus != OrderStatus.CONFIRMED && newStatus != OrderStatus.CANCELLED)
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                if (newStatus == OrderStatus.CONFIRMED) {
                    PaymentMethod method = order.getPayment().getPaymentMethod();
                    if (method == PaymentMethod.CASH) {
                        paymentService.markPaymentSucceeded(order);
                    } else if (order.getPayment().getStatus() != PaymentStatus.PAID) {
                        throw new AppException(ErrorCode.PAYMENT_NOT_COMPLETED);
                    }
                }
            }
            case CONFIRMED -> {
                if (newStatus != OrderStatus.PICKING && newStatus != OrderStatus.CANCELLED)
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
            }
            case PICKING -> {
                if (newStatus != OrderStatus.PICKED && newStatus != OrderStatus.CANCELLED)
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
            }

            case PICKED -> {
                if (newStatus != OrderStatus.CANCELLED)
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
            }
            case SHIPPING -> {
                if (newStatus != OrderStatus.DELIVERED)
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                if (proofImage == null || proofImage.isEmpty()) {
                    throw new AppException(ErrorCode.DELIVERY_PROOF_IMAGE_REQUIRED);
                }
                validateShipperOnDuty(currentUser);
                if (!order.getStaffSchedule().getStaff().getUserId().equals(currentUser.getUserId())){
                    throw new AppException(ErrorCode.NOT_THE_ASSIGNED_SHIPPER);
                }
            }
            case DELIVERED -> {
                if (newStatus != OrderStatus.COMPLETED)
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                if (order.getPayment().getStatus() != PaymentStatus.PAID) {
                    Payment payment = order.getPayment();
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
            }
            case COMPLETED, CANCELLED -> throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            paymentService.cancelPaymentForOrder(order);
        }

        if (newStatus == OrderStatus.DELIVERED) {
            MediaFile proof = fileService.uploadShippingProofImage(proofImage);
            proof.setOrder(order);
            order.getMediaFiles().add(proof);
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Override
    public OrderResponse requestCancelOrder(Long orderId, String cancelReason) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Payment payment = order.getPayment();
        order.setCancelReason(cancelReason);

        if (payment.getPaymentMethod() == PaymentMethod.CASH) {
            paymentService.cancelPaymentForOrder(order);
            order.setStatus(OrderStatus.CANCELLED);
        } else {
            paymentRepository.save(payment);
            order.setStatus(OrderStatus.CANCEL_REQUESTED);
        }

        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        return orderMapper.toOrderResponse(saved);
    }

    @Override
    public OrderResponse confirmCancelOrder(Long orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        paymentService.cancelPaymentForOrder(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        return orderMapper.toOrderResponse(saved);
    }

    @Override
    public List<PickingItemResponse> getPickingList(Long orderId) {
        checkLogin();
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.PICKING) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        return buildPickingList(order);
    }

    @Override
    public Page<OrderResponse> getOrder(Pageable pageable) {
        User user = getCurrentUser();
        Page<Order> orders = orderRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId(), pageable);
        return orders.map(orderMapper::toOrderResponse);
    }

    @Override
    public Page<OrderResponse> getAllOrder(Pageable pageable) {
        checkLogin();
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(orderMapper::toOrderResponse);
    }

    @Override
    public OrderResponse getOrder(Long orderId) {
        getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Scheduled(fixedRate = 60 * 1000)
    public void expirePendingOrders() {
        List<Order> expired = orderRepository
                .findByStatusAndPaymentExpiredAtBeforeAndPayment_PaymentMethod(OrderStatus.PENDING, LocalDateTime.now(), PaymentMethod.CARD);

        for (Order order : expired) {
            try {
                expireSingleOrder(order);
            } catch (Exception ex) {
                log.error("Lỗi khi expire order {}: {}", order.getOrderCode(), ex.getMessage());
            }
        }
    }

    private void expireSingleOrder(Order order) {
        Payment payment = order.getPayment();

        if (payment == null || payment.getPaymentMethod() != PaymentMethod.CARD) {
            return;
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Order {} đã PAID trước khi job expire chạy tới, bỏ qua", order.getOrderCode());
            return;
        }

        if (payment.getStripePaymentIntentId() != null) {
            try {
                paymentServiceImpl.cancelStripeIntent(payment.getStripePaymentIntentId());
            } catch (AppException ex) {
                if (ex.getErrorCode() == ErrorCode.PAYMENT_ALREADY_PAID) {
                    log.warn("Order {} đã thanh toán trước khi expire, bỏ qua", order.getOrderCode());
                    return;
                }
                log.error("Cancel Stripe intent thất bại cho order {}, vẫn expire local, cần đối soát: {}",
                        order.getOrderCode(), ex.getMessage());
            }
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        paymentService.releaseOrderStock(order);
        handlePaymentFailStreak(order.getUser(), order.getOrderCode());

        order.setStatus(OrderStatus.EXPIRED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    public void handlePaymentFailStreak(User user, String orderCode) {
        int streak = user.getPaymentFailStreak() + 1;
        user.setPaymentFailStreak(streak);

        if (streak >= MAX_CONSECUTIVE_FAILS) {
            if (user.getStatus() != UserStatus.SUSPENDED) {
                user.setStatus(UserStatus.SUSPENDED);
                log.warn("User {} bị SUSPENDED do {} lần liên tiếp không thanh toán", user.getUserId(), streak);
                emailService.sendAccountSuspendedEmail(user.getEmail(), streak);
            }
        } else {
            emailService.sendPaymentFailWarningEmail(user.getEmail(), orderCode, streak, MAX_CONSECUTIVE_FAILS);
        }

        userRepository.save(user);
    }

    private List<PickingItemResponse> buildPickingList(Order order) {
        List<PickingItemResponse> result = new ArrayList<>();
        for (OrderDetail detail : order.getOrderDetails()) {
            result.addAll(calculatePickingItems(detail.getProduct().getProductId(), detail.getQuantity()));
        }
        return result;
    }

    private List<PickingItemResponse> calculatePickingItems(UUID productId, int quantity) {
        List<ProductBatch> batches = getActiveBatchesByFefo(productId);
        int remaining = quantity;
        List<PickingItemResponse> result = new ArrayList<>();

        for (ProductBatch batch : batches) {
            if (remaining <= 0) break;
            int picked = Math.min(batch.getStockQuantity(), remaining);
            result.add(PickingItemResponse.builder()
                    .productId(batch.getProduct().getProductId())
                    .name(batch.getProduct().getName())
                    .imageUrl(getFirstImage(batch.getProduct()))
                    .expiryDate(batch.getExpiryDate())
                    .quantityToPick(picked)
                    .build());
            remaining -= picked;
        }

        if (remaining > 0) throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        return result;
    }


    private List<ProductBatch> getActiveBatchesByFefo(UUID productId) {
        return productBatchRepository
                .findByProduct_ProductIdAndStockQuantityGreaterThanAndStatusOrderByExpiryDateAscCreatedAtAscBatchCodeAsc(
                        productId, 0, ProductStatus.ACTIVE);
    }

    private String generateOrderCode() {
        return "OD" + ThreadLocalRandom.current().nextInt(100000, 1000000);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new AppException(ErrorCode.UNAUTHENTICATED);
        String userId = authentication.getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void checkLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName().equals("anonymousUser")) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    }


    private String getFirstImage(Product product) {
        if (product == null || product.getMediaFiles() == null || product.getMediaFiles().isEmpty()) {
            return null;
        }
        return product.getMediaFiles().getFirst().getFileUrl();
    }
    private void validateShipperOnDuty(User user) {
        if (user.getRole() != Role.STAFF || user.getStaffTask() != StaffTask.SHIPPER) {
            throw new AppException(ErrorCode.NOT_SHIPPER);
        }
        boolean onDutyToday = staffScheduleRepository.existsSchedule(
                user.getUserId(),
                LocalDate.now(),
                LocalTime.MIN,
                LocalTime.of(23, 59, 59),
                null,
                List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.WORKING)
        );
        if (!onDutyToday) {
            throw new AppException(ErrorCode.SHIPPER_NOT_ON_DUTY);
        }
    }
}