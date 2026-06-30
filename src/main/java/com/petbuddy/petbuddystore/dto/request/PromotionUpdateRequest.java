package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
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
    LocalDateTime startDate;
    LocalDateTime endDate;
    PromotionStatus status;
    List<PromotionDetailRequest> promotionDetails;
}