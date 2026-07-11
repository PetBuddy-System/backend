package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewUpdateRequest {

    @Min(value = 1, message = "RATING_INVALID")
    @Max(value = 5, message = "RATING_INVALID")
    Integer rating;

    @Size(max = 1000, message = "CONTENT_TOO_LONG")
    String content;

    Boolean anonymous;
}