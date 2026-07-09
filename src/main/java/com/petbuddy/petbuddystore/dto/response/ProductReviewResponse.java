package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewResponse {

    String reviewId;
    String productId;
    String productName;
    String userId;
    String fullName;
    Integer rating;
    String content;
    Boolean anonymous;
    ReviewStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}