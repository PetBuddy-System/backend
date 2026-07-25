package com.petbuddy.petbuddystore;

import com.petbuddy.petbuddystore.common.enums.PaymentStatus;
import com.petbuddy.petbuddystore.common.util.MomoSignatureUtil;
import com.petbuddy.petbuddystore.configuration.MomoConfig;
import com.petbuddy.petbuddystore.dto.request.MomoIpnRequest;
import com.petbuddy.petbuddystore.model.Payment;
import com.petbuddy.petbuddystore.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "SPRING_MAIL_HOST=localhost",
                "SPRING_MAIL_PORT=1025",
                "SPRING_MAIL_USERNAME=test",
                "SPRING_MAIL_PASSWORD=test",
                "SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/pet_buddy",
                "SPRING_DATASOURCE_USERNAME=root",
                "SPRING_DATASOURCE_PASSWORD=12345"
        }
)
class MomoIpnSimulationTest {

    @LocalServerPort
    int port;

    @Autowired
    private MomoConfig momoConfig;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void simulateSuccessfulMomoIpn() {

        String orderId = "OD634095-1784506504846";

        String requestId = UUID.randomUUID().toString();
        String transId = String.valueOf(System.currentTimeMillis());

        long amount = 211000L;

        String orderInfo = "Thanh toan don hang " + orderId;
        String orderType = "momo_wallet";
        String payType = "qr";
        String extraData = "";
        String message = "Successful.";
        int resultCode = 0;
        String responseTime = String.valueOf(Instant.now().toEpochMilli());

        System.out.println("DS_URL = " + System.getenv("SPRING_DATASOURCE_URL"));
        System.out.println("DS_URL_PROP = " + System.getProperty("SPRING_DATASOURCE_URL"));

        String rawSignature =
                "accessKey=" + momoConfig.getAccessKey()
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

        String signature = MomoSignatureUtil.hmacSHA256(
                rawSignature,
                momoConfig.getSecretKey()
        );

        MomoIpnRequest request = MomoIpnRequest.builder()
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

        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        String body = client.post()
                .uri("/pet-buddy/api/payments/ipn")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

        System.out.println(body);

        Optional<Payment> paymentOpt =
                paymentRepository.findByMomoOrderId(orderId);

        assertThat(paymentOpt).isPresent();

        Payment payment = paymentOpt.get();

        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.PAID);

        assertThat(payment.getMomoTransId())
                .isEqualTo(transId);

        assertThat(payment.getPaidAt())
                .isNotNull();
    }

}