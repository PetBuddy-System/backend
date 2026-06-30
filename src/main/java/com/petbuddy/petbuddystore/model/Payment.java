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

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    Order order;

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

    @Column(name = "stripe_session_id", unique = true)
    String stripeSessionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
