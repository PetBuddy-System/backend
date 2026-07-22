package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.dto.response.VnPayCreatePaymentResponse;
import com.petbuddy.petbuddystore.dto.response.VnPayRefundResponse;
import com.petbuddy.petbuddystore.model.Payment;
import com.petbuddy.petbuddystore.service.VnPayService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class VnPayServiceImpl implements VnPayService {
    @NonFinal
    @Value("${vnp.TmnCode}")
    String tmnCode;

    @NonFinal
    @Value("${vnp.HashSecret}")
    String hashSecret;

    @NonFinal
    @Value("${vnp.Url}")
    String payUrl;

    @NonFinal
    @Value("${vnp.refundUrl}")
    String refundUrl;

    @NonFinal
    @Value("${vnp.redirectUrl}")
    String returnUrl;

    @NonFinal
    @Value("${vnp.ipnUrl}")
    String ipnUrl;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public VnPayCreatePaymentResponse createPaymentUrl(Long amount, String txnRef, String orderInfo, String clientIp) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", clientIp);

        LocalDateTime now = LocalDateTime.now();
        String createDate = now.format(FMT);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(FMT));

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (Iterator<String> it = fieldNames.iterator(); it.hasNext(); ) {
            String name = it.next();
            String value = params.get(name);
            if (value == null || value.isEmpty()) continue;

            hashData.append(name).append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
            query.append(URLEncoder.encode(name, StandardCharsets.US_ASCII)).append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
            if (it.hasNext()) {
                hashData.append('&');
                query.append('&');
            }
        }

        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        String finalUrl = payUrl + "?" + query;

        return VnPayCreatePaymentResponse.builder()
                .payUrl(finalUrl)
                .txnRef(txnRef)
                .createDate(createDate)
                .build();
    }

    @Override
    public boolean verifySignature(Map<String, String> params, String receivedHash) {
        Map<String, String> copy = new HashMap<>(params);
        copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(copy.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (Iterator<String> it = fieldNames.iterator(); it.hasNext(); ) {
            String name = it.next();
            String value = copy.get(name);
            if (value == null || value.isEmpty()) continue;
            hashData.append(name).append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
            if (it.hasNext()) hashData.append('&');
        }

        String computedHash = hmacSHA512(hashSecret, hashData.toString());
        return computedHash.equalsIgnoreCase(receivedHash);
    }

    @Override
    public VnPayRefundResponse createRefund(Payment payment, BigDecimal refundAmount, String createBy) {
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        LocalDateTime now = LocalDateTime.now();
        String createDate = now.format(FMT);
        long amount = refundAmount.longValue() * 100;

        boolean isFullRefund = refundAmount.compareTo(payment.getAmount()) >= 0;
        String transactionType = isFullRefund ? "02" : "03";

        String orderInfo = "Hoan tien GD " + payment.getVnpayTxnRef();
        String transactionNo = payment.getVnpayTransactionNo() != null ? payment.getVnpayTransactionNo() : "0";
        String transactionDate = payment.getVnpayCreateDate();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_RequestId", requestId);
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "refund");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_TransactionType", transactionType);
        params.put("vnp_TxnRef", payment.getVnpayTxnRef());
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_TransactionNo", transactionNo);
        params.put("vnp_TransactionDate", transactionDate);
        params.put("vnp_CreateBy", createBy);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_IpAddr", "127.0.0.1");

        String hashData = String.join("|",
                requestId,
                params.get("vnp_Version"),
                params.get("vnp_Command"),
                tmnCode,
                transactionType,
                params.get("vnp_TxnRef"),
                params.get("vnp_Amount"),
                transactionNo,
                transactionDate,
                createBy,
                params.get("vnp_IpAddr"),
                orderInfo
        );
        String secureHash = hmacSHA512(hashSecret, hashData);
        params.put("vnp_SecureHash", secureHash);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(refundUrl, entity, Map.class);
            Map<String, Object> body = response.getBody();

            String responseCode = body != null ? String.valueOf(body.get("vnp_ResponseCode")) : "99";
            String message = body != null ? String.valueOf(body.get("vnp_Message")) : "Không có phản hồi";
            String refundTransNo = body != null ? String.valueOf(body.get("vnp_TransactionNo")) : null;

            return VnPayRefundResponse.builder()
                    .responseCode(responseCode)
                    .transactionNo(refundTransNo)
                    .message(message)
                    .success("00".equals(responseCode))
                    .build();
        } catch (Exception e) {
            log.error("Lỗi khi gọi VNPay refund API cho txnRef={}: {}", payment.getVnpayTxnRef(), e.getMessage(), e);
            return VnPayRefundResponse.builder()
                    .responseCode("99")
                    .success(false)
                    .message("Lỗi kết nối VNPay: " + e.getMessage())
                    .build();
        }
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Lỗi khi tạo HMAC SHA512 cho VNPay: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo chữ ký VNPay", e);
        }
    }
}
