package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnRequestResponse {
    // Thông tin yêu cầu
    Long returnRequestId;
    String returnCode;

    // Thông tin đơn hàng
    Long orderId;
    String orderCode;

    // Thông tin địa chỉ giao hàng
    String recipientName;
    String phoneNumber;
    String address;
    Double latitude;
    Double longitude;

    // Thông tin người dùng
    UserResponse requestedBy;
    UserResponse coordinator;
    UserResponse shipper;

    // Thông tin yêu cầu
    ReturnType type;
    ReturnReason reason;
    String description;
    ReturnStatus status;

    //Số lần lấy hàng thất bại (thêm mới)
    Integer pickupFailedCount;

    // Số lần giao hàng thất bại
    Integer deliveryFailedCount;

    // Thông tin hoàn tiền
    RefundMethod refundMethod;
    RefundStatus refundStatus;
    BigDecimal refundAmount;
    String bankName;
    String bankAccountNumber;
    String bankAccountHolder;

    // Ghi chú
    String staffNote;

    // Timeline
    LocalDateTime createdAt;
    LocalDateTime approvedAt;
    LocalDateTime pickingUpAt;
    LocalDateTime pickupFailedAt;
    LocalDateTime pickedUpAt;
    LocalDateTime returnedToStoreAt;
    LocalDateTime readyToDeliverAt;
    LocalDateTime deliveringAt;
    LocalDateTime deliveringFailedAt;
    LocalDateTime completedAt;
    LocalDateTime rejectedAt;
    LocalDateTime cancelledAt;
    LocalDateTime restockedAt;
    LocalDateTime updatedAt;

    // Danh sách sản phẩm và file
    List<ReturnItemResponse> returnItems;
    List<MediaFileResponse> mediaFiles;
}