package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.OrderStatus;
import com.petbuddy.petbuddystore.dto.request.CreateOrderRequest;
import com.petbuddy.petbuddystore.dto.response.OrderResponse;
import com.petbuddy.petbuddystore.dto.response.PickingItemResponse;
import com.petbuddy.petbuddystore.dto.response.StaffOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    void updateOrderStatus(Long orderId, OrderStatus status);
    Page<OrderResponse> getOrder(Pageable pageable);
    Page<StaffOrderResponse> getAllOrder(Pageable pageable);
    OrderResponse getOrder(Long orderId);
    List<PickingItemResponse> getPickingList(Long orderId);


}
