package com.petbuddy.petbuddystore.common.enums;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PICKING,
    PICKED,
    SHIPPING,
    DELIVERED,
    AWAITING_REDELIVERY,
    DELIVERY_FAILED,
    COORDINATOR_REVIEW,
    RETURNED_TO_WAREHOUSE,
    COMPLETED,
    CANCEL_REQUESTED,
    CANCELLED,
    EXPIRED
}
