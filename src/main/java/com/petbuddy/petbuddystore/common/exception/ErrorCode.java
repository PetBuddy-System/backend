package com.petbuddy.petbuddystore.common.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(9998, "Uncategorized error", HttpStatus.BAD_REQUEST),

    EMAIL_REQUIRED(1002, "Email is required", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1003, "Email is invalid", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1004, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    FULL_NAME_REQUIRED(1005, "Full name is required", HttpStatus.BAD_REQUEST),
    GENDER_REQUIRED(1006, "Gender is required", HttpStatus.BAD_REQUEST),
    DATE_OF_BIRTH_REQUIRED(1007, "Date of birth is required", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1008, "User with this email already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1009, "User does not exists", HttpStatus.NOT_FOUND),
    INVALID_DOB_FORMAT(1010, "Date of birth must be in format dd-MM-yyyy", HttpStatus.BAD_REQUEST),

    USER_NOT_VERIFIED(2001, "Please verify your email before logging in", HttpStatus.FORBIDDEN),
    USER_INACTIVE(2002, "User is inactive", HttpStatus.FORBIDDEN),
    USER_SUSPENDED(2003, "User has been suspended", HttpStatus.FORBIDDEN),
    USER_DELETED(2004, "User has been deleted", HttpStatus.GONE),
    UNAUTHENTICATED(2005, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(2006, "You do not have permission to access", HttpStatus.FORBIDDEN),

    OTP_EXPIRED(3001, "OTP has expired", HttpStatus.BAD_REQUEST),
    OTP_INVALID(3002, "OTP is invalid", HttpStatus.BAD_REQUEST),
    OTP_MAX_ATTEMPT(3003, "Too many attempts. Try again later", HttpStatus.BAD_REQUEST),
    OTP_RESEND_COOLDOWN(3004, "Please wait before requesting another OTP", HttpStatus.BAD_REQUEST),
    OTP_RATE_LIMIT(3005, "Too many OTP requests", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED(3006, "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_ALREADY_VERIFIED(3007, "Email has already verified", HttpStatus.BAD_REQUEST),
    OTP_REQUIRED(3008, "Otp is required", HttpStatus.BAD_REQUEST),
    PASSWORD_INCORRECT(3009, "Old password is incorrect", HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRM_NOT_MATCH(3010, "Password confirmation does not match", HttpStatus.BAD_REQUEST),
    PASSWORD_SAME_AS_OLD(3011, "New password must be different from old password", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(3012, "Password is required", HttpStatus.BAD_REQUEST),

    CATEGORY_NAME_REQUIRED(4101, "Category name is required", HttpStatus.BAD_REQUEST),
    CATEGORY_EXISTED(4102, "Category already exists", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(4103, "Category not found", HttpStatus.NOT_FOUND),
    CATEGORY_HAS_PRODUCTS(4104, "Cannot delete category because it is linked to products", HttpStatus.BAD_REQUEST),
    CATEGORY_DELETED(4105, "Category has been deleted", HttpStatus.BAD_REQUEST),
    CATEGORY_REQUIRED(4108, "Category is required", HttpStatus.BAD_REQUEST),


    PRODUCT_STOCK_INVALID(4205, "Product stock quantity must be greater than or equal to 0", HttpStatus.BAD_REQUEST),
    PRODUCT_EXISTED(4206, "Product already exists", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND(4207, "Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_DELETED(4208, "Product has been deleted", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_DELETED(4209, "Product is not deleted", HttpStatus.BAD_REQUEST),
    CATEGORY_ID_REQUIRED(4210, "Category id is required", HttpStatus.BAD_REQUEST),
    PRODUCT_STATUS_REQUIRED(4211, "Product status is required", HttpStatus.BAD_REQUEST),
    PRODUCT_IMAGE_REQUIRED(4212, "Product image is required", HttpStatus.BAD_REQUEST),
    EXPIRY_DATE_REQUIRED(4213, "Expiry date is required", HttpStatus.BAD_REQUEST),
    EXPIRY_DATE_INVALID(4214, "Expiry date must be in the future", HttpStatus.BAD_REQUEST),
    PRODUCT_OUT_OF_STOCK(4215, "Product out of stock", HttpStatus.BAD_REQUEST),
    INVALID_SORT_OPTION(4216, "Invalid sort option", HttpStatus.BAD_REQUEST),
    PRODUCT_IMAGE_LIMIT_EXCEEDED(4217, "Cannot upload more than 5 images for a product", HttpStatus.BAD_REQUEST),
    BATCH_REQUIRED(4218, "Batch list is required", HttpStatus.BAD_REQUEST),
    BATCH_LIMIT_EXCEEDED(4219, "Cannot create more than 10 batches at once", HttpStatus.BAD_REQUEST),
    BATCH_NOT_FOUND(4220, "Batch not found", HttpStatus.NOT_FOUND),
    PRODUCT_HAS_BATCHES(4221, "Cannot delete product because it has batches", HttpStatus.BAD_REQUEST),
    PRODUCT_INACTIVE(4222, "Product is inactive", HttpStatus.BAD_REQUEST),
    PRODUCT_PRICE_CHANGE(4216, "Product price has changed", HttpStatus.BAD_REQUEST),


    CART_EMPTY(5101, "Cart is empty", HttpStatus.BAD_REQUEST),
    CART_ITEM_NOT_FOUND(5102, "Cart item not found", HttpStatus.NOT_FOUND),
    INVALID_QUANTITY(5103, "Quantity must be greater than 0", HttpStatus.BAD_REQUEST),

    ORDER_NOT_FOUND(5201, "Order not found", HttpStatus.NOT_FOUND),
    INVALID_ORDER_STATUS(5202, "Invalid order status", HttpStatus.BAD_REQUEST),
    PICKING_LIST_NOT_FOUND(5203, "Picking list not found",HttpStatus.NOT_FOUND),
    PICKING_ALREADY_CREATED(5204, "Picking list already created", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(5205, "Insufficient stock",HttpStatus.BAD_REQUEST),

    VOUCHER_NOT_FOUND(5301, "Voucher not found", HttpStatus.NOT_FOUND),
    VOUCHER_CODE_EXISTED(5302, "Voucher code already exists", HttpStatus.BAD_REQUEST),
    VOUCHER_INVALID_DATE(5303, "Voucher date is invalid", HttpStatus.BAD_REQUEST),
    VOUCHER_EXPIRED(5304, "Voucher has expired",HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_STARTED(5305, "Voucher is not active yet",HttpStatus.BAD_REQUEST),
    VOUCHER_OUT_OF_USAGE(5306, "Voucher usage limit exceeded",HttpStatus.BAD_REQUEST),
    VOUCHER_MIN_ORDER_NOT_MET(5307, "Order value does not meet voucher requirement",HttpStatus.BAD_REQUEST),
    VOUCHER_USER_LIMIT_EXCEEDED(5308, "You have reached voucher usage limit",HttpStatus.BAD_REQUEST),
    VOUCHER_INVALID_STATUS(5309, "Voucher is inactive",HttpStatus.BAD_REQUEST),

    FILE_REQUIRED(7001, "File is required", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(7002, "File size is too large", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(7003, "Only JPG, PNG, WEBP images are allowed", HttpStatus.BAD_REQUEST),
    UPLOAD_FAILED(7004, "Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR),
    IMAGE_SIZE_INVALID(7005, "Upload maximum 3 photos", HttpStatus.BAD_REQUEST),
    DELETE_FILE_FAILED(7006, "Failed to delete file", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_EMPTY(7007, "File is empty", HttpStatus.BAD_REQUEST),
    IMPORT_FAILED(7008, "Failed to import file. Please check the file format and data.", HttpStatus.BAD_REQUEST),
    INVALID_EXCEL_TEMPLATE(7009, "Invalid Excel template. Please use the provided template.", HttpStatus.BAD_REQUEST),

    PET_NOT_EXISTED(8001, "Pet does not exist", HttpStatus.NOT_FOUND),
    CAGE_EXISTED(8002, "Cage has already existed", HttpStatus.BAD_REQUEST),
    CAGE_NOT_EXISTED(8003, "Cage does not exist", HttpStatus.NOT_FOUND),
    BLOG_EXISTED(8004, "Blog has already existed", HttpStatus.BAD_REQUEST),
    BLOG_NOT_EXISTED(8005, "Blog does not exist", HttpStatus.NOT_FOUND),

    CATALOG_NOT_FOUND(9001, "Service catalog not found", HttpStatus.NOT_FOUND),
    USER_NOT_STAFF(9002, "User is not staff", HttpStatus.FORBIDDEN),
    INVALID_WORKING_TIME(9003, "Working time is invalid", HttpStatus.BAD_REQUEST),
    STAFF_SCHEDULE_NOT_EXISTED(9004, "Staff schedule not found", HttpStatus.NOT_FOUND),
    CANNOT_UPDATE(9005, "Cannot update schedule started", HttpStatus.BAD_REQUEST),
    SAME_STAFF(9006, "Same staff already exists", HttpStatus.BAD_REQUEST),
    SCHEDULE_ALREADY_EXISTS(9007, "Staff already has a schedule in this time range", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE(9008, "Date range is invalid", HttpStatus.BAD_REQUEST),
    STAFF_ALREADY_ASSIGNED_TO_SCHEDULE(9009, "Staff already assigned to this schedule", HttpStatus.BAD_REQUEST),
    WORK_SCHEDULE_NOT_EXISTED(9010, "Work schedule does not exist", HttpStatus.NOT_FOUND),
    WORK_SCHEDULE_EXISTED(9011, "Work schedule already exists", HttpStatus.BAD_REQUEST),
    STAFF_LIST_EMPTY(9012, "Staff list is empty", HttpStatus.BAD_REQUEST),
    CANNOT_CHECKIN(9013, "Cannot check in", HttpStatus.BAD_REQUEST),
    CANNOT_CHECKOUT(9014, "Cannot check out", HttpStatus.BAD_REQUEST),


    ;

    int code;
    String message;
    HttpStatusCode httpStatusCode;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
