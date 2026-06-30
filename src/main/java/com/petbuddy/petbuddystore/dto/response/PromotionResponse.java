package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionResponse {
    UUID promotionId;
    String name;
    String description;
    LocalDateTime startDate;
    LocalDateTime endDate;
    PromotionStatus status;
    List<PromotionDetailResponse> promotionDetails;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
