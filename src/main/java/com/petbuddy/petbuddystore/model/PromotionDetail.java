package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "promotion_detail")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "promotion_detail_id")
    UUID promotionDetailId;

    @ManyToOne
    @JoinColumn(name = "promotion_id", nullable = false)
    Promotion promotion;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DiscountType discountType;

    @Column(nullable = false)
    BigDecimal discountValue;
}