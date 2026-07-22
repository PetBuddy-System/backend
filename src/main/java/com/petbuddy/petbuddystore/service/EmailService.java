package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EmailService {
    void sendVerifyEmailOtp(String toEmail, String otp);
    void sendForgotPasswordOtp(String toEmail, String otp);
    void sendPaymentFailWarningEmail(String email, String orderCode, int failCount, int maxFails);
    void sendAccountSuspendedEmail(String email, int failCount, LocalDateTime suspendedAt, LocalDateTime suspendedUntil);
    void sendBookingNotification(String to, String customerName, String serviceName, String date, String time, String totalAmount);
    void sendOrderPaymentSuccessEmail(String toEmail, Order order);
    void sendOrderBombedEmail(String toEmail, Order order);
    void sendRefundSuccessEmail(String toEmail, Order order, BigDecimal refundAmount);;
}
