package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.PaymentStatus;
import com.petbuddy.petbuddystore.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder_OrderId(Long orderId);

    List<Payment> findByStatusAndOrderIsNotNullAndPaidAtBetween(
            PaymentStatus status, LocalDateTime start, LocalDateTime end);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    boolean existsByOrder_OrderId(Long orderId);

    Optional<Payment> findByStripeRefundId(String stripeRefundId);
    Optional<Payment> findByMomoOrderId(String momoOrderId);
    Optional<Payment> findByVnpayTxnRef(String vnpayTxnRef);

}
