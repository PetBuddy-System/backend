package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id")
    UUID productId;


    @Column(name = "product_code", nullable = false, unique = true, length = 12)
    String productCode;

    @NotBlank(message = "PRODUCT_NAME_REQUIRED")
    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    String name;

    @Column(columnDefinition = "NVARCHAR(2000)")
    String description;

    @Column(columnDefinition = "NVARCHAR(1000)")
    private String ingredients;

    @Column(columnDefinition = "NVARCHAR(1000)")
    private String usageInstructions;

    @NotNull(message = "PRODUCT_PRICE_REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "PRODUCT_PRICE_INVALID")
    @Column(nullable = false)
    BigDecimal price;

    @Column(columnDefinition = "NVARCHAR(100)")
    String brandName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    ProductStatus status = ProductStatus.ACTIVE;

    LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "category_id")
    Category category;

//    @Builder.Default
//    List<String> imageUrls = new ArrayList<>();
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<MediaFile> mediaFiles = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ProductBatch> batches = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    @Builder.Default
    Long lastBatchSequence = 0L;

    @OneToMany(mappedBy = "product")
    @Builder.Default
    List<PromotionDetail> promotionDetails = new ArrayList<>();
}