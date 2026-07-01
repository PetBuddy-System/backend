package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Payment API", description = "Quản lý thanh toán")
public class PaymentController {
    PaymentService paymentService;

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(@PathVariable Long orderId) {

        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> handleStripeWebhook(
            HttpServletRequest request,
            @RequestHeader("Stripe-Signature") String sigHeader) throws IOException {

        byte[] payloadBytes = request.getInputStream().readAllBytes();
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);

        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok(ApiResponse.success("Webhook xử lý thành công"));
    }
}
