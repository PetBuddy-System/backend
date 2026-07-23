package com.petbuddy.petbuddystore.scheduler;

import com.petbuddy.petbuddystore.common.enums.OrderStatus;
import com.petbuddy.petbuddystore.common.enums.PaymentMethod;
import com.petbuddy.petbuddystore.common.enums.PaymentStatus;
import com.petbuddy.petbuddystore.common.enums.UserStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.model.Order;
import com.petbuddy.petbuddystore.model.Payment;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.OrderRepository;
import com.petbuddy.petbuddystore.repository.PaymentRepository;
import com.petbuddy.petbuddystore.repository.UserRepository;
import com.petbuddy.petbuddystore.service.EmailService;
import com.petbuddy.petbuddystore.service.PaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderExpirationScheduler {

    OrderRepository orderRepository;
    PaymentRepository paymentRepository;
    PaymentService paymentService;
    UserRepository userRepository;
    EmailService emailService;

    static int MAX_CONSECUTIVE_FAILS = 4;
    static int SUSPEND_DAYS = 7;

    @Scheduled(fixedRate = 60 * 1000)
    public void expirePendingOrders() {
        List<Order> expired = orderRepository
                .findByStatusAndPaymentExpiredAtBeforeAndPayment_PaymentMethodIn(
                        OrderStatus.PENDING, LocalDateTime.now(),
                        List.of(PaymentMethod.CARD, PaymentMethod.MOMO));

        for (Order order : expired) {
            try {
                expireSingleOrder(order);
            } catch (AppException ignored) {
            }
        }
    }

    private void expireSingleOrder(Order order) {
        Payment payment = order.getPayment();

        if (payment == null
                || (payment.getPaymentMethod() != PaymentMethod.CARD
                && payment.getPaymentMethod() != PaymentMethod.MOMO)) {
            return;
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PAID);
        }

        if (payment.getPaymentMethod() == PaymentMethod.CARD) {
            if (payment.getStripePaymentIntentId() != null) {
                try {
                    paymentService.cancelStripeIntent(payment.getStripePaymentIntentId());
                } catch (AppException ex) {
                    if (ex.getErrorCode() == ErrorCode.PAYMENT_ALREADY_PAID) {
                        throw ex;
                    }
                }
            }
        } else { // MOMO
            boolean actuallyPaid = paymentService.confirmMomoStatus(order);
            if (actuallyPaid) {
                throw new AppException(ErrorCode.PAYMENT_ALREADY_PAID);
            }
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        paymentService.releaseOrderStock(order);
        handlePaymentFailStreak(order.getUser(), order.getOrderCode());

        order.setStatus(OrderStatus.EXPIRED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    private void handlePaymentFailStreak(User user, String orderCode) {
        int streak = user.getPaymentFailStreak() + 1;
        user.setPaymentFailStreak(streak);

        if (streak >= MAX_CONSECUTIVE_FAILS) {
            if (user.getStatus() != UserStatus.SUSPENDED) {
                LocalDateTime suspendedAt = LocalDateTime.now();
                LocalDateTime suspendedUntil = suspendedAt.plusDays(SUSPEND_DAYS);
                user.setStatus(UserStatus.SUSPENDED);
                user.setSuspendedUntil(suspendedUntil);
                emailService.sendAccountSuspendedEmail(user.getEmail(), streak, suspendedAt, suspendedUntil);
            }
        } else {
            emailService.sendPaymentFailWarningEmail(user.getEmail(), orderCode, streak, MAX_CONSECUTIVE_FAILS);
        }

        userRepository.save(user);
    }
}