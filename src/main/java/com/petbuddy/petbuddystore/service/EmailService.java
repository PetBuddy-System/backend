package com.petbuddy.petbuddystore.service;

public interface EmailService {
    void sendVerifyEmailOtp(String toEmail, String otp);
    void sendForgotPasswordOtp(String toEmail, String otp);
    void sendPaymentFailWarningEmail(String email, String orderCode, int failCount, int maxFails);
    void sendAccountSuspendedEmail(String email, int failCount);
}
