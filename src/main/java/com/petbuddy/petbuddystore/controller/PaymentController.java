package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.MomoIpnRequest;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully"));
    }

    @PostMapping("/ipn")
    public ResponseEntity<ApiResponse<Void>> handleMomoIpn(@RequestBody MomoIpnRequest ipn) {
        try {
            paymentService.handleMomoIpn(ipn);
        } catch (Exception e) {
            log.error("Lỗi xử lý IPN MoMo cho orderId={}", ipn.getOrderId(), e);
        }
        return ResponseEntity.ok(ApiResponse.success("IPN xử lý thành công"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getAllPayments(pageable)));
    }

    @PutMapping("/method/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePaymentMethod(@PathVariable Long orderId, @RequestParam String paymentMethod) {
        return ResponseEntity.ok(ApiResponse.success("Payment method updated successfully",
                paymentService.changePaymentMethod(orderId,paymentMethod)));
    }

    @PostMapping("/{orderId}/momo/retry")
    public ResponseEntity<ApiResponse<PaymentResponse>> retryMomoPayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.retryMomoPayment(orderId)));
    }
}
