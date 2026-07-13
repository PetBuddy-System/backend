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
    Long returnRequestId;
    String returnCode;
    Long orderId;
    String orderCode;
    UserResponse requestedBy;
    UserResponse processedBy;
    ReturnType type;
    ReturnReason reason;
    String description;
    ReturnStatus status;
    RefundMethod refundMethod;
    RefundStatus refundStatus;
    BigDecimal refundAmount;
    String staffNote;
    String bankName;
    String bankAccountNumber;
    String bankAccountHolder;
    LocalDateTime createdAt;
    LocalDateTime processedAt;
    LocalDateTime completedAt;
    LocalDateTime updatedAt;
    LocalDateTime restockedAt;
    List<ReturnItemResponse> returnItems;
    List<MediaFileResponse> mediaFiles;
}
