package com.petbuddy.petbuddystore;

import com.petbuddy.petbuddystore.common.enums.PaymentStatus;
import com.petbuddy.petbuddystore.common.util.MomoSignatureUtil;
import com.petbuddy.petbuddystore.configuration.MomoConfig;
import com.petbuddy.petbuddystore.dto.request.MomoIpnRequest;
import com.petbuddy.petbuddystore.model.Payment;
import com.petbuddy.petbuddystore.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import org.springframework.http.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MomoIpnSimulationTest {
    @LocalServerPort
    int port;

    @Autowired
    MomoConfig momoConfig;

    TestRestTemplate restTemplate = new TestRestTemplate();

    @Autowired
    PaymentRepository paymentRepository;

    @Test
    void simulateSuccessfulMomoIpn() {
        // ---- THAY orderId này bằng momoOrderId thật bạn đã tạo qua createQrPayment ----
        // Lưu ý: đây là field "momoOrderId" (String) trong entity Payment,
        // KHÔNG phải paymentId hay orderId (Long) của Order.
        String orderId = "ORDER_ID_CAN_THAY";
        String requestId = UUID.randomUUID().toString();
        String transId = String.valueOf(System.currentTimeMillis());
        long amount = 100000; // phải khớp amount lúc tạo payment
        String orderInfo = "Thanh toan don hang " + orderId;
        String orderType = "momo_wallet";
        String payType = "qr";
        String extraData = "";
        String message = "Successful.";
        int resultCode = 0; // 0 = thành công theo MoMo
        String responseTime = String.valueOf(Instant.now().toEpochMilli());

        String rawSignature = "accessKey=" + momoConfig.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&message=" + message
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&orderType=" + orderType
                + "&partnerCode=" + momoConfig.getPartnerCode()
                + "&payType=" + payType
                + "&requestId=" + requestId
                + "&responseTime=" + responseTime
                + "&resultCode=" + resultCode
                + "&transId=" + transId;

        String signature = MomoSignatureUtil.hmacSHA256(rawSignature, momoConfig.getSecretKey());

        MomoIpnRequest ipn = MomoIpnRequest.builder()
                .partnerCode(momoConfig.getPartnerCode())
                .orderId(orderId)
                .requestId(requestId)
                .amount(amount)
                .orderInfo(orderInfo)
                .orderType(orderType)
                .transId(transId)
                .resultCode(resultCode)
                .message(message)
                .payType(payType)
                .responseTime(responseTime)
                .extraData(extraData)
                .signature(signature)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MomoIpnRequest> entity = new HttpEntity<>(ipn, headers);

        String url = "http://localhost:" + port + "/api/payments/ipn";
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        System.out.println("IPN response status: " + response.getStatusCode());
        System.out.println("IPN response body: " + response.getBody());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Optional<Payment> paymentOpt = paymentRepository.findByMomoOrderId(orderId);
        assertThat(paymentOpt).isPresent();

        Payment payment = paymentOpt.get();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getMomoTransId()).isEqualTo(transId);
        assertThat(payment.getPaidAt()).isNotNull();

        System.out.println("Payment sau IPN: status=" + payment.getStatus()
                + ", momoTransId=" + payment.getMomoTransId()
                + ", paidAt=" + payment.getPaidAt());
    }
}
