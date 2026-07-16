package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReviewRequest {

    @Min(value = 1, message = "REVIEW_RATING_INVALID")
    @Max(value = 5, message = "REVIEW_RATING_INVALID")
    private Integer rating;

    @NotBlank(message = "REVIEW_CONTENT_REQUIRED")
    @Size(max = 1000, message = "REVIEW_CONTENT_TOO_LONG")
    private String content;

    private Boolean anonymous;
}