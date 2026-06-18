package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_batches", uniqueConstraints = {@UniqueConstraint(columnNames = "batch_code")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "batch_id")
    UUID batchId;

    @Column(name = "batch_code", nullable = false, unique = true, length = 30)
    String batchCode;

    @NotNull(message = "PRODUCT_STOCK_REQUIRED")
    @Min(value = 0, message = "PRODUCT_STOCK_INVALID")
    @Column(nullable = false)
    Integer stockQuantity;

    @Column(name = "expiry_date")
    LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    ProductStatus status = ProductStatus.ACTIVE;

    LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}