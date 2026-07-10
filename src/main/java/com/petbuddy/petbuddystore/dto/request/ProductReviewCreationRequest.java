package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewCreationRequest {

    @NotNull(message = "RATING_REQUIRED")
    @Min(value = 1, message = "RATING_INVALID")
    @Max(value = 5, message = "RATING_INVALID")
    Integer rating;

    @NotBlank(message = "CONTENT_REQUIRED")
    @Size(max = 1000, message = "CONTENT_TOO_LONG")
    String content;

    Boolean anonymous = false;
}