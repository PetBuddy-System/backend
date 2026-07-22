package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.response.VnPayCreatePaymentResponse;
import com.petbuddy.petbuddystore.dto.response.VnPayRefundResponse;
import com.petbuddy.petbuddystore.model.Payment;

import java.math.BigDecimal;
import java.util.Map;

public interface VnPayService {
    VnPayCreatePaymentResponse createPaymentUrl(Long amount, String txnRef, String orderInfo, String clientIp);
    boolean verifySignature(Map<String, String> params, String receivedHash);
    VnPayRefundResponse createRefund(Payment payment, BigDecimal refundAmount, String createBy);
}
