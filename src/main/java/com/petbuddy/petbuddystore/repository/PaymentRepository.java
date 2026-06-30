package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder_OrderId(Long orderId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    boolean existsByOrder_OrderId(Long orderId);
}
