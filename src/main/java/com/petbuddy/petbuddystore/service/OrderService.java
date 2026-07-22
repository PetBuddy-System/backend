package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.OrderStatus;
import com.petbuddy.petbuddystore.dto.request.CreateOrderRequest;
import com.petbuddy.petbuddystore.dto.request.UpdateOrderRequest;
import com.petbuddy.petbuddystore.dto.response.OrderResponse;
import com.petbuddy.petbuddystore.dto.response.PickingItemResponse;
import com.petbuddy.petbuddystore.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    void updateOrderStatus(Long orderId, OrderStatus status, MultipartFile proofImage);
    Page<OrderResponse> getOrder(Pageable pageable);
    Page<OrderResponse> getAllOrder(Pageable pageable);
    OrderResponse getOrder(Long orderId);
    List<PickingItemResponse> getPickingList(Long orderId);
    OrderResponse updateOrder(Long orderId, UpdateOrderRequest request);
    OrderResponse requestCancelOrder(Long orderId, String cancelReason);
    OrderResponse confirmCancelOrder(Long orderId);
    Order getOrderEntityById(Long orderId);
    boolean hasUserPurchasedProduct(String userId, UUID productId);
    OrderResponse reportDeliveryFailed(Long orderId, String reason);
    OrderResponse confirmReturnedToWarehouse(Long orderId);
}
