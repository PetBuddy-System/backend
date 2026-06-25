package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    Long orderId;

    @Column(unique = true)
    String orderCode;

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne
    @JoinColumn(name = "voucher_id")
    Voucher voucher;

    @Column(columnDefinition = "NVARCHAR(255)")
    String recipientName;

    String phoneNumber;

    @Column(columnDefinition = "NVARCHAR(255)")
    String address;

    BigDecimal shippingFee;

    BigDecimal totalAmount;

    BigDecimal discountAmount;

    BigDecimal finalAmount;

    @Column(columnDefinition = "NVARCHAR(500)")
    String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<OrderDetail> orderDetails = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "payment_id")
    Payment payment;

    @Enumerated(EnumType.STRING)
    OrderStatus status;

    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}