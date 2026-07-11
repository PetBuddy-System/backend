package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.RefundMethod;
import com.petbuddy.petbuddystore.common.enums.ReturnStatus;
import com.petbuddy.petbuddystore.common.enums.ReturnType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReturnFilterRequest {
    private ReturnStatus status;
    private String orderCode;
    private String returnCode;
    private RefundMethod refundMethod;
    private ReturnType type;
    private LocalDate fromDate;
    private LocalDate toDate;
    /** keyword: tìm theo orderCode, returnCode, fullName, email */
    private String keyword;
}
