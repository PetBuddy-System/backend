package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.MomoIpnRequest;
import com.petbuddy.petbuddystore.dto.response.MomoCreatePaymentResponse;

public interface MomoService {
    MomoCreatePaymentResponse createQrPayment(Long amount, String orderId, String orderInfo);
    boolean verifyIpnSignature(MomoIpnRequest ipn);
    MomoCreatePaymentResponse queryTransactionStatus(String orderId, String requestId);
}
