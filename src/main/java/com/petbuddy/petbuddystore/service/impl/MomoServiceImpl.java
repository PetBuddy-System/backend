package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.common.util.MomoSignatureUtil;
import com.petbuddy.petbuddystore.configuration.MomoConfig;
import com.petbuddy.petbuddystore.dto.request.MomoCreatePaymentRequest;
import com.petbuddy.petbuddystore.dto.request.MomoIpnRequest;
import com.petbuddy.petbuddystore.dto.response.MomoCreatePaymentResponse;
import com.petbuddy.petbuddystore.service.MomoService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MomoServiceImpl implements MomoService {

    MomoConfig momoConfig;
    RestTemplate restTemplate = new RestTemplate();

    @Override
    public MomoCreatePaymentResponse createQrPayment(Long amount, String orderId, String orderInfo) {
        String requestId = UUID.randomUUID().toString();
        String requestType = "captureWallet";
        String extraData = "";

        String rawSignature = "accessKey=" + momoConfig.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + momoConfig.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoConfig.getPartnerCode()
                + "&redirectUrl=" + momoConfig.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=" + requestType;

        String signature = MomoSignatureUtil.hmacSHA256(rawSignature, momoConfig.getSecretKey());

        MomoCreatePaymentRequest request = MomoCreatePaymentRequest.builder()
                .partnerCode(momoConfig.getPartnerCode())
                .partnerName("PetBuddy Store")
                .storeId("PetBuddyStore")
                .requestId(requestId)
                .amount(amount)
                .orderId(orderId)
                .orderInfo(orderInfo)
                .redirectUrl(momoConfig.getRedirectUrl())
                .ipnUrl(momoConfig.getIpnUrl())
                .lang("vi")
                .requestType(requestType)
                .autoCapture(String.valueOf(true))
                .extraData(extraData)
                .signature(signature)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MomoCreatePaymentRequest> entity = new HttpEntity<>(request, headers);

        try {
            MomoCreatePaymentResponse response = restTemplate.postForObject(
                    momoConfig.getEndpoint(), entity, MomoCreatePaymentResponse.class);

            if (response == null || response.getResultCode() == null || response.getResultCode() != 0) {
                log.error("MoMo trả lỗi khi tạo payment orderId={}: {}",
                        orderId, response != null ? response.getMessage() : "null response");
                throw new AppException(ErrorCode.PAYMENT_MOMO_ERROR);
            }
            return response;
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi khi gọi MoMo API cho orderId={}", orderId, ex);
            throw new AppException(ErrorCode.PAYMENT_MOMO_ERROR);
        }
    }

    @Override
    public boolean verifyIpnSignature(MomoIpnRequest ipn) {
        String rawSignature = "accessKey=" + momoConfig.getAccessKey()
                + "&amount=" + ipn.getAmount()
                + "&extraData=" + ipn.getExtraData()
                + "&message=" + ipn.getMessage()
                + "&orderId=" + ipn.getOrderId()
                + "&orderInfo=" + ipn.getOrderInfo()
                + "&orderType=" + ipn.getOrderType()
                + "&partnerCode=" + ipn.getPartnerCode()
                + "&payType=" + ipn.getPayType()
                + "&requestId=" + ipn.getRequestId()
                + "&responseTime=" + ipn.getResponseTime()
                + "&resultCode=" + ipn.getResultCode()
                + "&transId=" + ipn.getTransId();

        String expected = MomoSignatureUtil.hmacSHA256(rawSignature, momoConfig.getSecretKey());
        return expected.equals(ipn.getSignature());
    }
}