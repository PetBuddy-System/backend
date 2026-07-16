package com.petbuddy.petbuddystore.service;

import java.time.LocalDateTime;

public interface EmailService {
    void sendVerifyEmailOtp(String toEmail, String otp);
    void sendForgotPasswordOtp(String toEmail, String otp);
    void sendPaymentFailWarningEmail(String email, String orderCode, int failCount, int maxFails);
    void sendAccountSuspendedEmail(String email, int failCount, LocalDateTime suspendedAt, LocalDateTime suspendedUntil);
    void sendBookingNotification(String to, String customerName, String serviceName, String date, String time, String totalAmount);
}
