package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionUpdateRequest {

    String name;
    String description;

    @Future(message = "PROMOTION_START_DATE_MUST_BE_FUTURE")
    LocalDateTime startDate;

    @Future(message = "PROMOTION_END_DATE_MUST_BE_AFTER_START_DATE")
    LocalDateTime endDate;

    PromotionStatus status;
    List<PromotionDetailRequest> promotionDetails;

    @Size(max = 500, message = "PROMOTION_REASON_TOO_LONG")
    String reason;

    @Size(max = 1000, message = "PROMOTION_NOTE_TOO_LONG")
    String note;
}