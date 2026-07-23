package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionRequest {

    @NotBlank(message = "PROMOTION_NAME_REQUIRED")
    String name;

    String description;

    @NotNull(message = "PROMOTION_START_DATE_REQUIRED")
    @Future(message = "PROMOTION_START_DATE_MUST_BE_FUTURE")
    LocalDateTime startDate;

    @NotNull(message = "PROMOTION_END_DATE_REQUIRED")
    @Future(message = "PROMOTION_END_DATE_MUST_BE_AFTER_START_DATE")
    LocalDateTime endDate;

    PromotionStatus status;

    List<PromotionDetailRequest> promotionDetails;
}