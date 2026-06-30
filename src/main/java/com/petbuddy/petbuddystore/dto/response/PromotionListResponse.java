package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionListResponse {
    UUID promotionId;
    String name;
    String description;
    LocalDateTime startDate;
    LocalDateTime endDate;
    PromotionStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}