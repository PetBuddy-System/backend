package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.ReviewStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_reviews", uniqueConstraints = {@UniqueConstraint(name = "uk_review_user_product", columnNames = {"user_id", "product_id", "deleted_at"})})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "review_id")
    UUID reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Min(value = 1, message = "REVIEW_RATING_INVALID")
    @Max(value = 5, message = "REVIEW_RATING_INVALID")
    @Column(nullable = false)
    Integer rating;

    @NotBlank(message = "REVIEW_CONTENT_REQUIRED")
    @Column(nullable = false, columnDefinition = "NVARCHAR(1000)")
    String content;

    @Builder.Default
    @Column(nullable = false)
    Boolean anonymous = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    ReviewStatus status = ReviewStatus.ACTIVE;

    LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}