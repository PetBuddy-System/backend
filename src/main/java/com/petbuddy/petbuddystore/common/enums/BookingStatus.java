package com.petbuddy.petbuddystore.common.enums;

public enum BookingStatus {
    PENDING_PAYMENT,    //đơn hàng đặt thành công chờ duyệt
    FAILED,
    PENDING_ACCEPTANCE, //đơn hàng thanh toán thành công
    ACCEPTED,
    IN_PROGRESS,        //đơn đang thực hiện sau khi duyệt
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED,
    WAITING_STAFF
}

