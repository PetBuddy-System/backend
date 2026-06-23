package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.DiscountType;
import com.petbuddy.petbuddystore.common.enums.OrderStatus;
import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.enums.VoucherStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.CreateOrderRequest;
import com.petbuddy.petbuddystore.dto.response.*;
import com.petbuddy.petbuddystore.mapper.OrderMapper;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.*;
import com.petbuddy.petbuddystore.service.CartService;
import com.petbuddy.petbuddystore.service.OrderService;
import com.petbuddy.petbuddystore.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderServiceImpl implements OrderService {

    OrderRepository orderRepository;
    ProductBatchRepository productBatchRepository;
    ProductService productService;
    CartService cartService;
    OrderMapper orderMapper;
    UserRepository userRepository;
    UserVoucherRepository userVoucherRepository;
    VoucherRepository voucherRepository;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        checkLogin();

        User user = getCurrentUser();

        List<CartItemResponse> cartItems = cartService.getCart().getItems();

        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .user(user)
                .recipientName(request.getUserName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .note(request.getNote())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        List<OrderDetail> orderDetails = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItemResponse item : cartItems) {
            Product product = productService.getProductEntityById(item.getProductId());

            int availableStock = productBatchRepository.findAvailableStockByProductId(product.getProductId());

            if (availableStock < item.getQuantity()) {throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);}

            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
//                    .productImage(product.getImageUrls().getFirst())
                    .unitPrice(item.getPrice())
                    .quantity(item.getQuantity())
                    .totalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build();

            total = total.add(detail.getTotalPrice());
            orderDetails.add(detail);
        }

        BigDecimal finalAmount = total;
        Voucher voucher;

        if(request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()){
            voucher = voucherRepository.findByVoucherCode(request.getVoucherCode())
                    .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

            validateVoucher(voucher, user, total);

            BigDecimal discountAmount = calculateDiscount(voucher, total);
            finalAmount = total.subtract(discountAmount);

            voucher.setUsedCount(voucher.getUsedCount() + 1);
            voucherRepository.save(voucher);

            UserVouchers userVoucher = UserVouchers.builder()
                    .user(user)
                    .voucher(voucher)
                    .usedAt(LocalDateTime.now())
                    .build();
            userVoucherRepository.save(userVoucher);
        }

        order.setOrderDetails(orderDetails);
        order.setTotalAmount(total);
        order.setFinalAmount(finalAmount);
        orderRepository.save(order);
//        cartService.clearCart();
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        checkLogin();

        Order order = findOrder(orderId);
        OrderStatus currentStatus = order.getStatus();

        switch (currentStatus) {
            case PENDING -> {
                if (newStatus != OrderStatus.CONFIRMED && newStatus != OrderStatus.CANCELED) {
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                }
            }

            case CONFIRMED -> {
                if (newStatus != OrderStatus.PICKING && newStatus != OrderStatus.CANCELED) {
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                }
            }

            case PICKING -> {
                if (newStatus != OrderStatus.SHIPPING && newStatus != OrderStatus.CANCELED) {
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                }

                if (newStatus == OrderStatus.SHIPPING) {
                    for (OrderDetail detail : order.getOrderDetails()) {
                        deductStockByFefo(
                                detail.getProduct().getProductId(),
                                detail.getQuantity()
                        );
                    }
                }
            }

            case SHIPPING -> {
                if (newStatus != OrderStatus.DELIVERED) {
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                }
            }

            case DELIVERED -> {
                if (newStatus != OrderStatus.COMPLETED) {
                    throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
                }
            }

            case COMPLETED, CANCELED -> {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
            }
        }
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Override
    public List<PickingItemResponse> getPickingList(Long orderId) {
        checkLogin();
        Order order = findOrder(orderId);
        if(order.getStatus() != OrderStatus.PICKING){
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
    public Page<StaffOrderResponse> getAllOrder(Pageable pageable) {
        checkLogin();
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(orderMapper::toStaffOrderResponse);
    }

    @Override
    public OrderResponse getOrder(Long orderId) {
        getCurrentUser();
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return orderMapper.toOrderResponse(order);
    }

    private List<PickingItemResponse> buildPickingList(Order order) {
        List<PickingItemResponse> result = new ArrayList<>();

        for(OrderDetail detail : order.getOrderDetails()) {
            result.addAll(calculatePickingItems(detail.getProduct().getProductId(), detail.getQuantity()));
        }

        return result;
    }

    private List<PickingItemResponse> calculatePickingItems(UUID productId, int quantity) {
        List<ProductBatch> batches = getActiveBatchesByFefo(productId);

        int remaining = quantity;
        List<PickingItemResponse> result = new ArrayList<>();

        for (ProductBatch batch : batches) {
            if (remaining <= 0) {
                break;
            }

            int picked = Math.min(batch.getStockQuantity(), remaining);

            result.add(PickingItemResponse.builder()
                            .productId(batch.getProduct().getProductId())
                            .name(batch.getProduct().getName())
                            .expiryDate(batch.getExpiryDate())
                            .quantityToPick(picked)
                            .build()
            );

            remaining -= picked;
        }

        if(remaining > 0){
            throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

        return result;
    }



    private void deductStockByFefo(UUID productId, int quantity) {
        List<ProductBatch> batches = getActiveBatchesByFefo(productId);
        List<ProductBatch> updatedBatches = new ArrayList<>();
        int remaining = quantity;

        for (ProductBatch batch : batches) {
            if (remaining <= 0) break;

            int picked = Math.min(batch.getStockQuantity(), remaining);
            batch.setStockQuantity(batch.getStockQuantity() - picked);
            updatedBatches.add(batch);
            remaining -= picked;
        }

        if (remaining > 0) {
            throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }
        productBatchRepository.saveAll(updatedBatches);
    }

    private List<ProductBatch> getActiveBatchesByFefo(UUID productId) {
        return productBatchRepository
                .findByProduct_ProductIdAndStockQuantityGreaterThanAndStatusOrderByExpiryDateAscCreatedAtAscBatchCodeAsc(
                        productId,
                        0,
                        ProductStatus.ACTIVE
                );
    }

    private String generateOrderCode() {return "OD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));}

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {throw new AppException(ErrorCode.UNAUTHENTICATED);}
        String userId = authentication.getName();
        return userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void checkLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderAmount) {
        BigDecimal discount;

        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100));
            if (voucher.getMaxDiscount() != null) {
                discount = discount.min(voucher.getMaxDiscount());
            }
        } else {
            discount = voucher.getDiscountValue();
        }

        return discount.min(orderAmount);
    }

    private void validateVoucher(Voucher voucher, User user, BigDecimal totalAmount) {
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            throw new AppException(ErrorCode.VOUCHER_INVALID_STATUS);
        }

        if (now.isBefore(voucher.getStartAt())) {
            throw new AppException(ErrorCode.VOUCHER_NOT_STARTED);
        }

        if (now.isAfter(voucher.getExpiredAt())) {
            throw new AppException(ErrorCode.VOUCHER_EXPIRED);
        }

        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new AppException(ErrorCode.VOUCHER_OUT_OF_USAGE);
        }

        if (voucher.getMinOrderValue() != null && totalAmount.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.VOUCHER_MIN_ORDER_NOT_MET);
        }

        long userUsedCount = userVoucherRepository.countByUserAndVoucher(user, voucher);
        if (voucher.getPerUserLimit() != null && userUsedCount >= voucher.getPerUserLimit()) {
            throw new AppException(ErrorCode.VOUCHER_USER_LIMIT_EXCEEDED);
        }
    }
}