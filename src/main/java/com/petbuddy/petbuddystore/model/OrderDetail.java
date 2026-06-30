package com.petbuddy.petbuddystore.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_detail")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_detail_id")
    Long orderDetailId;

    @ManyToOne
    @JoinColumn(name = "order_id")
    Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

    @Column(columnDefinition = "NVARCHAR(255)")
    String productName;

    String productImage;

    BigDecimal unitPrice;

    Integer quantity;

    BigDecimal totalPrice;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;
}