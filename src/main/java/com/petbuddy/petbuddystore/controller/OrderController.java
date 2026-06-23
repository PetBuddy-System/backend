package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.OrderStatus;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.CreateOrderRequest;
import com.petbuddy.petbuddystore.dto.response.OrderResponse;
import com.petbuddy.petbuddystore.dto.response.PickingItemResponse;
import com.petbuddy.petbuddystore.dto.response.StaffOrderResponse;
import com.petbuddy.petbuddystore.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Order API", description = "Quản lý đơn hàng")
public class OrderController {

    OrderService orderService;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", orderService.createOrder(createOrderRequest)));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(@ParameterObject @PageableDefault(
            sort = "createdAt",
            direction = Sort.Direction.DESC
    ) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orderService.getOrder(pageable)));
    }

    @PreAuthorize("hasRole('CUSTOMER') or hasRole('STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", orderService.getOrder(id)));
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('CUSTOMER')")
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {

        orderService.updateOrderStatus(orderId, status);

        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", null));
    }

    @PreAuthorize("hasRole('STAFF')")
    @GetMapping("/{id}/picking-list")
    public ResponseEntity<ApiResponse<List<PickingItemResponse>>> getPickingList(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Picking list retrieved successfully", orderService.getPickingList(id)));
    }

    @PreAuthorize("hasRole('STAFF')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<StaffOrderResponse>>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orderService.getAllOrder(pageable)));
    }
}
