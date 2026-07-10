package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.ReviewStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductReviewManagerResponse {

    // Review info
    String reviewId;
    Integer rating;
    String content;
    Boolean anonymous;
    ReviewStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;

    // Product info
    UUID productId;
    String productCode;
    String productName;

    // User info
    String userId;
    String userEmail;
    String userFullName;
    String userAvatar;
}