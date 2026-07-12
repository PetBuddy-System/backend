package com.petbuddy.petbuddystore.common.enums;

public enum ReturnReason {

    DAMAGED,

    WRONG_PRODUCT,

    MISSING_ITEM,

    EXPIRED,

    CUSTOMER_CHANGED_MIND,

    OTHER,

    OUT_OF_STOCK   //sản phẩm heets hàng nếu user chọn đổi hàng kh có nữa thì qua báo lỗi kêu user chọn cái này sẽ đc hoàng tiền 100%
}