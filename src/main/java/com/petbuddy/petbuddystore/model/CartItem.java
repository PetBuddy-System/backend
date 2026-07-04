package com.petbuddy.petbuddystore.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID cartItemId;
    @Column(name = "product_name")
    String productName;
    String description;
    BigDecimal price;
    @Column(name = "sale_price")
    BigDecimal salePrice;
    @Column(name = "image_url")
    String imageUrl;
    @ManyToOne
    @JoinColumn(name = "cart_id")
    Cart cart;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;
    Integer quantity;
    BigDecimal subtotal;
}