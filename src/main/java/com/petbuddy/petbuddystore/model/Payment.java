package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.PaymentMethod;
import com.petbuddy.petbuddystore.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long paymentId;

    @OneToOne
    @JoinColumn(name = "order_id", unique = true)
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    PaymentMethod paymentMethod;

    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    PaymentStatus status;

    @Column(name = "paid_at")
    LocalDateTime paidAt;

    @Column(name = "stripe_payment_intent_id", unique = true)
    String stripePaymentIntentId;

    @Column(name = "stripe_client_secret")
    String stripeClientSecret;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "stripe_refund_id", unique = true)
    String stripeRefundId;

    @Column(name = "refunded_at")
    LocalDateTime refundedAt;

    @Column(name = "cancel_reason")
    String cancelReason;

    @Column(name = "refunded_amount")
    private BigDecimal refundedAmount;
}
