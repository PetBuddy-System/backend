package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.model.Order;
import com.petbuddy.petbuddystore.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private void sendEmail(String toEmail, String otp, String template, String subject) {
        try {
            Context context = new Context();
            context.setVariable("otp", otp);

            String html = templateEngine.process(template, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    @Override
    public void sendVerifyEmailOtp(String toEmail, String otp) {
        sendEmail(toEmail, otp, "verify-email", "Verify your email - PetBuddy");
    }

    @Override
    public void sendForgotPasswordOtp(String toEmail, String otp) {
        sendEmail(toEmail, otp, "forgot-password", "Reset your password - PetBuddy");
    }

    private void sendHtmlEmail(String toEmail, String subject, String template, Map<String, Object> variables) {
        try {
            Context context = new Context();
            variables.forEach(context::setVariable);

            String html = templateEngine.process(template, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    @Override
    public void sendPaymentFailWarningEmail(String toEmail, String orderCode, int failCount, int maxFails) {
        String subject = "Cảnh báo: Đơn hàng " + orderCode + " chưa được thanh toán";
        sendHtmlEmail(toEmail, subject, "payment-fail-warning", Map.of(
                "orderCode", orderCode,
                "failCount", failCount,
                "maxFails", maxFails
        ));
    }

    @Override
    public void sendAccountSuspendedEmail(String toEmail, int failCount, LocalDateTime suspendedAt, LocalDateTime suspendedUntil) {
        String subject = "Tài khoản của bạn đã bị tạm khóa";
        sendHtmlEmail(toEmail, subject, "account-suspended", Map.of(
                "failCount", failCount,
                "suspendedAt", suspendedAt,
                "suspendedUntil", suspendedUntil
        ));
    }

    @Override
    public void sendBookingNotification(
            String to,
            String customerName,
            String serviceName,
            String date,
            String time,
            String totalAmount
    ) {
        try {
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("serviceName", serviceName);
            context.setVariable("date", date);
            context.setVariable("time", time);
            context.setVariable("totalAmount", totalAmount);

            String html = templateEngine.process("booking-notification", context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("PetBuddy - Booking notification");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.info("Booking notification fallback: to={}, customerName={}, serviceName={}, date={}, time={}, totalAmount={}",
                    to, customerName, serviceName, date, time, totalAmount);
        }
    }

    @Override
    public void sendOrderPaymentSuccessEmail(String toEmail, Order order) {
        try {
            Context context = new Context();
            context.setVariable("recipientName", order.getRecipientName());
            context.setVariable("orderCode", order.getOrderCode());
            context.setVariable("orderDetails", order.getOrderDetails());
            context.setVariable("totalAmount", order.getTotalAmount());
            context.setVariable("discountAmount", order.getDiscountAmount());
            context.setVariable("shippingFee", order.getShippingFee());
            context.setVariable("finalAmount", order.getFinalAmount());

            String html = templateEngine.process("payment-success", context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Đặt hàng thành công - Đơn hàng " + order.getOrderCode());
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
    @Override
    public void sendOrderBombedEmail(String toEmail, Order order) {
        String subject = "Đơn hàng " + order.getOrderCode() + " đã bị hủy do không liên lạc được";
        sendHtmlEmail(toEmail, subject, "order-bombed", Map.of(
                "recipientName", order.getRecipientName(),
                "orderCode", order.getOrderCode(),
                "failCount", order.getDeliveryFailCount(),
                "finalAmount", order.getFinalAmount(),
                "cancelReason", order.getCancelReason() != null ? order.getCancelReason() : "Không liên lạc được với khách hàng"
        ));
    }
}
