package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.OrderStatus;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.CancelOrderRequest;
import com.petbuddy.petbuddystore.dto.request.CreateOrderRequest;
import com.petbuddy.petbuddystore.dto.request.DeliveryFailedRequest;
import com.petbuddy.petbuddystore.dto.request.UpdateOrderRequest;
import com.petbuddy.petbuddystore.dto.response.OrderResponse;
import com.petbuddy.petbuddystore.dto.response.PickingItemResponse;
import com.petbuddy.petbuddystore.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PreAuthorize("hasRole('CUSTOMER') ")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(@ParameterObject @PageableDefault(
            sort = "createdAt",
            direction = Sort.Direction.DESC
    ) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orderService.getOrder(pageable)));
    }

    @PreAuthorize("hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", orderService.getOrder(id)));
    }

    @PreAuthorize("hasRole('STAFF') and hasAuthority('TASK_SHIPPER') or hasAuthority('TASK_COORDINATOR') or hasRole('CUSTOMER')")
    @PatchMapping(value = "/{orderId}/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable Long orderId,
            @RequestParam OrderStatus status,
            @RequestParam(required = false) MultipartFile proofImage) {

        orderService.updateOrderStatus(orderId, status, proofImage);

        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", null));
    }

    @PreAuthorize("hasRole('STAFF') and hasAuthority('TASK_COORDINATOR')")
    @GetMapping("/{id}/picking-list")
    public ResponseEntity<ApiResponse<List<PickingItemResponse>>> getPickingList(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Picking list retrieved successfully", orderService.getPickingList(id)));
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orderService.getAllOrder(pageable)));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(@PathVariable Long orderId,
                                                                  @RequestBody @Valid UpdateOrderRequest request){
        return ResponseEntity.ok(ApiResponse.success("Order updated successfully",
                orderService.updateOrder(orderId, request)));
    }
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/{orderId}/cancel-request")
    public ResponseEntity<ApiResponse<OrderResponse>> requestCancel(
            @PathVariable Long orderId,
            @RequestBody @Valid CancelOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Create request cancel successfully",orderService.requestCancelOrder(orderId, request.getCancelReason())));
    }

    @PreAuthorize("hasRole('STAFF') and hasAuthority('TASK_COORDINATOR')")
    @PostMapping("/{orderId}/cancel-confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmCancel(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Cancel order successfully",orderService.confirmCancelOrder(orderId)));
    }
    @PreAuthorize("hasRole('STAFF') and hasAuthority('TASK_SHIPPER')")
    @PostMapping("/{orderId}/delivery-failed")
    public ResponseEntity<ApiResponse<OrderResponse>> reportDeliveryFailed(
            @PathVariable Long orderId,
            @RequestBody @Valid DeliveryFailedRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận không liên lạc được khách hàng",
                orderService.reportDeliveryFailed(orderId, request.getReason())));
    }
    @PreAuthorize("hasRole('STAFF') and hasAuthority('TASK_COORDINATOR')")
    @PostMapping("/{orderId}/returned-to-warehouse")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmReturnedToWarehouse(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Xác nhận đã trả hàng về kho thành công",
                orderService.confirmReturnedToWarehouse(orderId)));
    }
}
